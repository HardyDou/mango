package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentChannelOrderCloseServiceTest {

    private PaymentOrderMapper paymentOrderMapper;
    private PaymentBusinessOrderMapper businessOrderMapper;
    private PaymentApplicationMapper applicationMapper;
    private PaymentOperationAuditService auditService;
    private PaymentNotificationService notificationService;
    private PaymentOrderStatusFlowService statusFlowService;
    private PaymentExceptionOrderRecordService exceptionOrderRecordService;
    private PaymentChannelOrderCloseService service;

    @BeforeEach
    void setUp() {
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        businessOrderMapper = mock(PaymentBusinessOrderMapper.class);
        applicationMapper = mock(PaymentApplicationMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        notificationService = mock(PaymentNotificationService.class);
        statusFlowService = mock(PaymentOrderStatusFlowService.class);
        exceptionOrderRecordService = mock(PaymentExceptionOrderRecordService.class);
        service = new PaymentChannelOrderCloseService(
                paymentOrderMapper,
                businessOrderMapper,
                applicationMapper,
                new PaymentOrderStateService(),
                auditService,
                notificationService,
                statusFlowService,
                exceptionOrderRecordService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("closePaymentOrder should close PAYING payment and business order")
    void closePaymentOrder_paying_closesPaymentAndBusinessOrder() {
        PaymentOrderEntity paymentOrder = paymentOrder("PAYING", 0);
        PaymentBusinessOrderEntity businessOrder = businessOrder("PAYING", 0L);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder);
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder);
        when(paymentOrderMapper.closeOpenPaymentOrder(1L, 370001L)).thenReturn(1);
        when(businessOrderMapper.closeOpenBusinessOrder(1L, 360001L)).thenReturn(1);
        when(applicationMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(application());
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrderVO("CLOSED"));

        PaymentChannelOrderCloseService.CloseResult result = service.closePaymentOrder("PO202606060001");

        assertThat(result.payOrderNo()).isEqualTo("PO202606060001");
        assertThat(result.status()).isEqualTo("CLOSED");
        assertThat(result.changed()).isTrue();
        verify(paymentOrderMapper).closeOpenPaymentOrder(1L, 370001L);
        verify(businessOrderMapper).closeOpenBusinessOrder(1L, 360001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CLOSE_PAYMENT_ORDER,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                "PO202606060001",
                PaymentOperationAuditService.RESULT_SUCCESS);
        ArgumentCaptor<PaymentApplication> applicationCaptor = ArgumentCaptor.forClass(PaymentApplication.class);
        ArgumentCaptor<PaymentBusinessOrderEntity> businessOrderCaptor = ArgumentCaptor.forClass(PaymentBusinessOrderEntity.class);
        ArgumentCaptor<PaymentOrderVO> paymentOrderCaptor = ArgumentCaptor.forClass(PaymentOrderVO.class);
        verify(notificationService).notifyPaymentAfterCommit(
                applicationCaptor.capture(), businessOrderCaptor.capture(), paymentOrderCaptor.capture());
        assertThat(applicationCaptor.getValue().getAppId()).isEqualTo("app_openapi");
        assertThat(businessOrderCaptor.getValue().getBizOrderNo()).isEqualTo("BO202606060001");
        assertThat(paymentOrderCaptor.getValue().getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(paymentOrderCaptor.getValue().getStatus()).isEqualTo("CLOSED");
        verify(exceptionOrderRecordService, never()).createIfAbsent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("closeExpiredPaymentOrder should close order and create timeout exception")
    void closeExpiredPaymentOrder_paying_createsTimeoutException() {
        PaymentOrderEntity paymentOrder = paymentOrder("PAYING", 0);
        PaymentBusinessOrderEntity businessOrder = businessOrder("PAYING", 0L);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder);
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder);
        when(paymentOrderMapper.closeOpenPaymentOrder(1L, 370001L)).thenReturn(1);
        when(businessOrderMapper.closeOpenBusinessOrder(1L, 360001L)).thenReturn(1);
        when(applicationMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(application());
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrderVO("CLOSED"));

        PaymentChannelOrderCloseService.CloseResult result = service.closeExpiredPaymentOrder("PO202606060001");

        assertThat(result.changed()).isTrue();
        verify(exceptionOrderRecordService).createIfAbsent(
                eq(1L),
                eq("PO202606060001"),
                eq(PaymentExceptionOrderRecordService.TYPE_PAY_TIMEOUT),
                eq(PaymentExceptionOrderRecordService.SEVERITY_MEDIUM),
                eq("支付订单超过有效支付时间未收到通道成功结果，已关闭并等待人工核对"),
                isNull());
    }

    @Test
    @DisplayName("closePaymentOrder should be idempotent for already closed payment order")
    void closePaymentOrder_closed_returnsWithoutMutation() {
        PaymentOrderEntity paymentOrder = paymentOrder("CLOSED", 0);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder);

        PaymentChannelOrderCloseService.CloseResult result = service.closePaymentOrder("PO202606060001");

        assertThat(result.status()).isEqualTo("CLOSED");
        assertThat(result.changed()).isFalse();
        verify(businessOrderMapper, never()).selectCashierBusinessOrder(1L, 360001L);
        verify(paymentOrderMapper, never()).closeOpenPaymentOrder(1L, 370001L);
        verify(auditService, never()).record(
                PaymentOperationAuditService.ACTION_CLOSE_PAYMENT_ORDER,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                "PO202606060001",
                PaymentOperationAuditService.RESULT_SUCCESS);
        verify(notificationService, never()).notifyPaymentAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("closePaymentOrder should reject successful payment order")
    void closePaymentOrder_success_rejectsClose() {
        PaymentOrderEntity paymentOrder = paymentOrder("SUCCESS", 1);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder);

        assertThatThrownBy(() -> service.closePaymentOrder("PO202606060001"))
                .isInstanceOf(BizException.class)
                .hasMessage("只有未支付或支付中的订单允许关单");
    }

    @Test
    @DisplayName("closePaymentOrder should reject business order with paid amount")
    void closePaymentOrder_paidBusinessOrder_rejectsClose() {
        PaymentOrderEntity paymentOrder = paymentOrder("PAYING", 0);
        PaymentBusinessOrderEntity businessOrder = businessOrder("PAYING", 9900L);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder);
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder);

        assertThatThrownBy(() -> service.closePaymentOrder("PO202606060001"))
                .isInstanceOf(BizException.class)
                .hasMessage("业务订单已有支付金额，不允许关单");
        verify(paymentOrderMapper, never()).closeOpenPaymentOrder(1L, 370001L);
        verify(businessOrderMapper, never()).closeOpenBusinessOrder(1L, 360001L);
    }

    private PaymentOrderEntity paymentOrder(String status, Integer successFlag) {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(370001L);
        order.setTenantId(1L);
        order.setPayOrderNo("PO202606060001");
        order.setBusinessOrderId(360001L);
        order.setAmount(9900L);
        order.setStatus(status);
        order.setSuccessFlag(successFlag);
        return order;
    }

    private PaymentBusinessOrderEntity businessOrder(String status, Long paidAmount) {
        PaymentBusinessOrderEntity order = new PaymentBusinessOrderEntity();
        order.setId(360001L);
        order.setTenantId(1L);
        order.setBizOrderNo("BO202606060001");
        order.setAppCode("app_openapi");
        order.setStatus(status);
        order.setPaidAmount(paidAmount);
        order.setDelFlag(0);
        return order;
    }

    private PaymentApplication application() {
        PaymentApplication application = new PaymentApplication();
        application.setTenantId(1L);
        application.setAppId("app_openapi");
        application.setAppSecret("openapi-secret");
        return application;
    }

    private PaymentOrderVO paymentOrderVO(String status) {
        PaymentOrderVO order = new PaymentOrderVO();
        order.setId(370001L);
        order.setPayOrderNo("PO202606060001");
        order.setBusinessOrderId(360001L);
        order.setBizOrderNo("BO202606060001");
        order.setAppId("app_openapi");
        order.setAmount(9900L);
        order.setCurrency("CNY");
        order.setStatus(status);
        order.setChannelCode("MANGO_PAY");
        return order;
    }
}
