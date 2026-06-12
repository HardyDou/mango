package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.vo.PaymentChannelCallbackResultVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentChannelCallbackServiceTest {

    private PaymentOrderMapper paymentOrderMapper;
    private PaymentRefundOrderMapper refundOrderMapper;
    private PaymentBusinessOrderMapper businessOrderMapper;
    private PaymentApplicationMapper applicationMapper;
    private PaymentTransactionFlowMapper transactionFlowMapper;
    private PaymentNotificationService notificationService;
    private PaymentOrderStatusFlowService statusFlowService;
    private PaymentDuplicatePaymentService duplicatePaymentService;
    private PaymentDuplicateRefundCompletionService duplicateRefundCompletionService;
    private PaymentObservabilityService observabilityService;
    private PaymentExceptionOrderService exceptionOrderService;
    private PaymentNumberService numberService;
    private PaymentChannelCallbackService service;

    @BeforeEach
    void setUp() {
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        refundOrderMapper = mock(PaymentRefundOrderMapper.class);
        businessOrderMapper = mock(PaymentBusinessOrderMapper.class);
        applicationMapper = mock(PaymentApplicationMapper.class);
        transactionFlowMapper = mock(PaymentTransactionFlowMapper.class);
        notificationService = mock(PaymentNotificationService.class);
        statusFlowService = mock(PaymentOrderStatusFlowService.class);
        duplicatePaymentService = mock(PaymentDuplicatePaymentService.class);
        duplicateRefundCompletionService = mock(PaymentDuplicateRefundCompletionService.class);
        observabilityService = mock(PaymentObservabilityService.class);
        exceptionOrderService = mock(PaymentExceptionOrderService.class);
        numberService = mock(PaymentNumberService.class);
        when(numberService.next(PaymentNumberService.PAY_FLOW_NO)).thenReturn("PF2026060600000001");
        when(numberService.next(PaymentNumberService.PAY_REFUND_FLOW_NO)).thenReturn("RF2026060600000001");
        service = new PaymentChannelCallbackService(
                paymentOrderMapper,
                refundOrderMapper,
                businessOrderMapper,
                applicationMapper,
                transactionFlowMapper,
                new PaymentOrderStateService(),
                notificationService,
                statusFlowService,
                duplicatePaymentService,
                duplicateRefundCompletionService,
                observabilityService,
                exceptionOrderService,
                numberService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("handle should advance paying payment callback to success and notify business")
    void handle_paymentSuccess_advancesOrderAndNotifies() {
        LocalDateTime eventTime = LocalDateTime.now();
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder("PAYING"));
        when(paymentOrderMapper.updatePayingCallbackResult(1L, 370001L, "SUCCESS", 1, eventTime, "CH202606060001"))
                .thenReturn(1);
        when(businessOrderMapper.markCashierPaySuccess(1L, 360001L, 9900L)).thenReturn(1);
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrderVO("SUCCESS"));
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);

        PaymentChannelCallbackResultVO result = service.handle(paymentCommand("SUCCESS", eventTime));

        assertThat(result.getChanged()).isTrue();
        assertThat(result.getOrderNo()).isEqualTo("PO202606060001");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getFlowNo()).startsWith("PF");
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getFlowType()).isEqualTo("PAY_SUCCESS");
        assertThat(flowCaptor.getValue().getAmount()).isEqualTo(9900L);
        verify(businessOrderMapper).markCashierPaySuccess(1L, 360001L, 9900L);
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT),
                eq(370001L),
                eq("PO202606060001"),
                eq("PAYING"),
                eq("SUCCESS"),
                eq(PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK),
                eq("CH202606060001"),
                eq(eventTime),
                eq("通道支付回调推进支付订单状态"));
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS),
                eq(360001L),
                eq("BO202606060001"),
                eq("PAYING"),
                eq("PAID"),
                eq(PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK),
                eq("CH202606060001"),
                eq(eventTime),
                eq("通道支付回调确认支付成功"));
        verify(notificationService).notifyPaymentAfterCommit(any(PaymentApplication.class), any(PaymentBusinessOrderEntity.class), any(PaymentOrderVO.class));
        verify(observabilityService).logSummary(
                eq("CHANNEL_PAYMENT_CALLBACK"),
                eq("PO202606060001"),
                eq("SUCCESS"),
                eq(9900L),
                eq("MANGO_PAY"),
                any(Long.class),
                eq("CHANGED"));
    }

    @Test
    @DisplayName("handle should advance paying payment callback to failed without success flow")
    void handle_paymentFailed_advancesOrderOnly() {
        LocalDateTime eventTime = LocalDateTime.now();
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder("PAYING"));
        when(paymentOrderMapper.updatePayingCallbackResult(1L, 370001L, "FAILED", 0, null, "CH202606060001"))
                .thenReturn(1);
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(paymentOrderMapper.selectPaymentOrderById(1L, 370001L)).thenReturn(paymentOrderVO("FAILED"));

        PaymentChannelCallbackResultVO result = service.handle(paymentCommand("FAILED", eventTime));

        assertThat(result.getChanged()).isTrue();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getFlowNo()).isNull();
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(businessOrderMapper, never()).markCashierPaySuccess(any(), any(), any());
        verify(exceptionOrderService).createIfAbsent(
                eq(1L),
                eq("PO202606060001"),
                eq(PaymentExceptionOrderService.TYPE_CHANNEL_FAILED),
                eq(PaymentExceptionOrderService.SEVERITY_HIGH),
                eq("通道支付回调返回失败状态，支付订单已失败并等待人工核对失败原因"),
                eq(eventTime));
        verify(notificationService).notifyPaymentAfterCommit(any(PaymentApplication.class), any(PaymentBusinessOrderEntity.class), any(PaymentOrderVO.class));
    }

    @Test
    @DisplayName("handle should return idempotent payment result for terminal order")
    void handle_paymentTerminal_returnsIdempotentResult() {
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder("SUCCESS"));
        when(paymentOrderMapper.selectLatestFlowNo(1L, 370001L)).thenReturn("FLOW202606060001");

        PaymentChannelCallbackResultVO result = service.handle(paymentCommand("SUCCESS", LocalDateTime.now()));

        assertThat(result.getChanged()).isFalse();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getFlowNo()).isEqualTo("FLOW202606060001");
        verify(paymentOrderMapper, never()).updatePayingCallbackResult(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyPaymentAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("handle should register exception when payment terminal callback conflicts with local terminal status")
    void handle_paymentTerminalConflict_registersExceptionAndAcknowledges() {
        LocalDateTime eventTime = LocalDateTime.now();
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder("SUCCESS"));
        when(paymentOrderMapper.selectLatestFlowNo(1L, 370001L)).thenReturn("FLOW202606060001");

        PaymentChannelCallbackResultVO result = service.handle(paymentCommand("FAILED", eventTime));

        assertThat(result.getChanged()).isFalse();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getFlowNo()).isEqualTo("FLOW202606060001");
        assertThat(result.getMessage()).contains("已登记异常订单");
        verify(exceptionOrderService).createIfAbsent(
                eq(1L),
                eq("PO202606060001"),
                eq(PaymentExceptionOrderService.TYPE_CHANNEL_FAILED),
                eq(PaymentExceptionOrderService.SEVERITY_HIGH),
                eq("通道支付回调终态与本地支付订单终态不一致，本地状态：SUCCESS，通道状态：FAILED"),
                eq(eventTime));
        verify(paymentOrderMapper, never()).updatePayingCallbackResult(any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).notifyPaymentAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("handle should process duplicate successful payment instead of throwing duplicate key error")
    void handle_paymentDuplicateSuccess_processesDuplicatePayment() {
        LocalDateTime eventTime = LocalDateTime.now();
        PaymentOrderEntity order = paymentOrder("PAYING");
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(order);
        when(paymentOrderMapper.updatePayingCallbackResult(1L, 370001L, "SUCCESS", 1, eventTime, "CH202606060001"))
                .thenThrow(new DuplicateKeyException("duplicate success"));
        when(duplicatePaymentService.handleDuplicateSuccess(
                eq(1L),
                eq(order),
                eq(eventTime),
                eq(PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK),
                eq("CH202606060001"),
                eq("CH202606060001")))
                .thenReturn(new PaymentDuplicatePaymentService.DuplicatePaymentResult(
                        "PO202606060001", "DUPLICATE_REFUNDING", false, "RO202606060001", null));
        when(paymentOrderMapper.selectLatestFlowNo(1L, 370001L)).thenReturn("RFLOW202606060001");

        PaymentChannelCallbackResultVO result = service.handle(paymentCommand("SUCCESS", eventTime));

        assertThat(result.getChanged()).isTrue();
        assertThat(result.getStatus()).isEqualTo("DUPLICATE_REFUNDING");
        assertThat(result.getFlowNo()).isEqualTo("RFLOW202606060001");
        assertThat(result.getMessage()).isEqualTo("重复成功支付已发起自动退款，等待通道结果");
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(businessOrderMapper, never()).markCashierPaySuccess(any(), any(), any());
        verify(notificationService, never()).notifyPaymentAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("handle should reject mismatched payment callback amount")
    void handle_paymentAmountMismatch_rejects() {
        PaymentChannelCallbackCommand command = paymentCommand("SUCCESS", LocalDateTime.now());
        command.setAmount(8800L);
        when(paymentOrderMapper.selectByTenantAndPayOrderNo(1L, "PO202606060001")).thenReturn(paymentOrder("PAYING"));

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_AMOUNT_INVALID.getCode());
        verify(paymentOrderMapper, never()).updatePayingCallbackResult(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("handle should advance refund callback to success and notify business")
    void handle_refundSuccess_advancesRefundAndNotifies() {
        LocalDateTime eventTime = LocalDateTime.now();
        when(refundOrderMapper.selectByTenantAndRefundOrderNo(1L, "RO202606060001")).thenReturn(refundOrder("REFUNDING"));
        when(refundOrderMapper.updateRefundingQueryResult(1L, 380001L, "SUCCESS", eventTime)).thenReturn(1);
        when(businessOrderMapper.updateRefundProgress(1L, 360001L, 3300L)).thenReturn(1);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);

        PaymentChannelCallbackResultVO result = service.handle(refundCommand("SUCCESS", eventTime));

        assertThat(result.getChanged()).isTrue();
        assertThat(result.getOrderNo()).isEqualTo("RO202606060001");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getFlowNo()).startsWith("RF");
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getFlowType()).isEqualTo("REFUND_SUCCESS");
        assertThat(flowCaptor.getValue().getAmount()).isEqualTo(3300L);
        verify(businessOrderMapper).updateRefundProgress(1L, 360001L, 3300L);
        verify(notificationService).notifyRefundAfterCommit(any(PaymentApplication.class), any(PaymentBusinessOrderEntity.class), any(PaymentRefundOrderVO.class));
    }

    @Test
    @DisplayName("handle should reject refund success when business refund progress CAS fails")
    void handle_refundSuccessBusinessCasFailed_rejectsWithoutFlowOrNotification() {
        LocalDateTime eventTime = LocalDateTime.now();
        when(refundOrderMapper.selectByTenantAndRefundOrderNo(1L, "RO202606060001")).thenReturn(refundOrder("REFUNDING"));
        when(refundOrderMapper.updateRefundingQueryResult(1L, 380001L, "SUCCESS", eventTime)).thenReturn(1);
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());
        when(businessOrderMapper.updateRefundProgress(1L, 360001L, 3300L)).thenReturn(0);

        assertThatThrownBy(() -> service.handle(refundCommand("SUCCESS", eventTime)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED.getCode());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, never()).notifyRefundAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("handle should reject refund callback when channel refund number mismatches")
    void handle_refundChannelRefundNoMismatch_rejectsBeforeStateChange() {
        PaymentRefundOrderVO refundOrder = refundOrder("REFUNDING");
        refundOrder.setChannelRefundNo("CRF202606060001");
        when(refundOrderMapper.selectByTenantAndRefundOrderNo(1L, "RO202606060001")).thenReturn(refundOrder);
        PaymentChannelCallbackCommand command = refundCommand("SUCCESS", LocalDateTime.now());
        command.setChannelRefundNo("CRF_WRONG");

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(BizException.class)
                .hasMessage("通道退款单号不匹配");
        verify(refundOrderMapper, never()).updateRefundingQueryResult(any(), any(), any(), any());
        verify(businessOrderMapper, never()).updateRefundProgress(any(), any(), any());
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, never()).notifyRefundAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("handle should allow only one success side effect when refund callbacks race")
    void handle_refundSuccessRace_allowsOnlyOneSuccessSideEffect() {
        LocalDateTime eventTime = LocalDateTime.now();
        when(refundOrderMapper.selectByTenantAndRefundOrderNo(1L, "RO202606060001"))
                .thenReturn(refundOrder("REFUNDING"), refundOrder("REFUNDING"));
        when(refundOrderMapper.updateRefundingQueryResult(1L, 380001L, "SUCCESS", eventTime))
                .thenReturn(1, 0);
        when(businessOrderMapper.updateRefundProgress(1L, 360001L, 3300L)).thenReturn(1);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());

        PaymentChannelCallbackResultVO result = service.handle(refundCommand("SUCCESS", eventTime));

        assertThat(result.getChanged()).isTrue();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThatThrownBy(() -> service.handle(refundCommand("SUCCESS", eventTime)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode());
        verify(refundOrderMapper, times(2)).updateRefundingQueryResult(1L, 380001L, "SUCCESS", eventTime);
        verify(businessOrderMapper, times(1)).updateRefundProgress(1L, 360001L, 3300L);
        verify(transactionFlowMapper, times(1)).insert(any(PaymentTransactionFlowEntity.class));
        verify(notificationService, times(1)).notifyRefundAfterCommit(
                any(PaymentApplication.class),
                any(PaymentBusinessOrderEntity.class),
                any(PaymentRefundOrderVO.class));
    }

    @Test
    @DisplayName("handle should advance refund callback to failed without success side effects")
    void handle_refundFailed_advancesRefundOnly() {
        LocalDateTime eventTime = LocalDateTime.now();
        when(refundOrderMapper.selectByTenantAndRefundOrderNo(1L, "RO202606060001")).thenReturn(refundOrder("REFUNDING"));
        when(refundOrderMapper.updateRefundingQueryResult(1L, 380001L, "FAILED", null)).thenReturn(1);
        when(applicationMapper.selectOne(any())).thenReturn(application());
        when(businessOrderMapper.selectCashierBusinessOrder(1L, 360001L)).thenReturn(businessOrder());

        PaymentChannelCallbackResultVO result = service.handle(refundCommand("FAILED", eventTime));

        assertThat(result.getChanged()).isTrue();
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getFlowNo()).isNull();
        verify(transactionFlowMapper, never()).insert(any(PaymentTransactionFlowEntity.class));
        verify(businessOrderMapper, never()).updateRefundProgress(any(), any(), any());
        verify(exceptionOrderService).createIfAbsent(
                eq(1L),
                eq("RO202606060001"),
                eq(PaymentExceptionOrderService.TYPE_REFUND_MISMATCH),
                eq(PaymentExceptionOrderService.SEVERITY_HIGH),
                eq("通道退款回调返回失败状态，退款订单已失败并等待人工核对退款结果"),
                eq(eventTime));
        verify(notificationService).notifyRefundAfterCommit(any(PaymentApplication.class), any(PaymentBusinessOrderEntity.class), any(PaymentRefundOrderVO.class));
    }

    @Test
    @DisplayName("handle should register exception when refund terminal callback conflicts with local terminal status")
    void handle_refundTerminalConflict_registersExceptionAndAcknowledges() {
        LocalDateTime eventTime = LocalDateTime.now();
        when(refundOrderMapper.selectByTenantAndRefundOrderNo(1L, "RO202606060001")).thenReturn(refundOrder("SUCCESS"));
        when(refundOrderMapper.selectLatestFlowNo(1L, 380001L)).thenReturn("RFLOW202606060001");

        PaymentChannelCallbackResultVO result = service.handle(refundCommand("FAILED", eventTime));

        assertThat(result.getChanged()).isFalse();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getFlowNo()).isEqualTo("RFLOW202606060001");
        assertThat(result.getMessage()).contains("已登记异常订单");
        verify(exceptionOrderService).createIfAbsent(
                eq(1L),
                eq("RO202606060001"),
                eq(PaymentExceptionOrderService.TYPE_REFUND_MISMATCH),
                eq(PaymentExceptionOrderService.SEVERITY_HIGH),
                eq("通道退款回调终态与本地退款订单终态不一致，本地状态：SUCCESS，通道状态：FAILED"),
                eq(eventTime));
        verify(refundOrderMapper, never()).updateRefundingQueryResult(any(), any(), any(), any());
        verify(notificationService, never()).notifyRefundAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("handle should reject processing refund callback")
    void handle_refundProcessing_rejects() {
        when(refundOrderMapper.selectByTenantAndRefundOrderNo(1L, "RO202606060001")).thenReturn(refundOrder("REFUNDING"));

        assertThatThrownBy(() -> service.handle(refundCommand("PROCESSING", LocalDateTime.now())))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode());
    }

    private PaymentChannelCallbackCommand paymentCommand(String status, LocalDateTime eventTime) {
        PaymentChannelCallbackCommand command = new PaymentChannelCallbackCommand();
        command.setCallbackType("PAYMENT");
        command.setChannelCode("MANGO_PAY");
        command.setPayOrderNo("PO202606060001");
        command.setChannelTradeNo("CH202606060001");
        command.setChannelMerchantNo("MCH202606060001");
        command.setChannelStatus(status);
        command.setAmount(9900L);
        command.setEventTime(eventTime);
        return command;
    }

    private PaymentChannelCallbackCommand refundCommand(String status, LocalDateTime eventTime) {
        PaymentChannelCallbackCommand command = new PaymentChannelCallbackCommand();
        command.setCallbackType("REFUND");
        command.setChannelCode("MANGO_PAY");
        command.setRefundOrderNo("RO202606060001");
        command.setChannelRefundNo("CRF202606060001");
        command.setChannelMerchantNo("MCH202606060001");
        command.setChannelStatus(status);
        command.setAmount(3300L);
        command.setEventTime(eventTime);
        return command;
    }

    private PaymentOrderEntity paymentOrder(String status) {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(370001L);
        order.setTenantId(1L);
        order.setPayOrderNo("PO202606060001");
        order.setBusinessOrderId(360001L);
        order.setChannelCode("MANGO_PAY");
        order.setChannelMerchantNo("MCH202606060001");
        order.setContractId(331001L);
        order.setAmount(9900L);
        order.setStatus(status);
        order.setSuccessFlag("SUCCESS".equals(status) ? 1 : 0);
        return order;
    }

    private PaymentRefundOrderVO refundOrder(String status) {
        PaymentRefundOrderVO order = new PaymentRefundOrderVO();
        order.setId(380001L);
        order.setRefundOrderNo("RO202606060001");
        order.setBizRefundNo("RF202606060001");
        order.setPaymentOrderId(370001L);
        order.setBusinessOrderId(360001L);
        order.setAppId("app_openapi");
        order.setPayOrderNo("PO202606060001");
        order.setChannelCode("MANGO_PAY");
        order.setChannelMerchantNo("MCH202606060001");
        order.setChannelRefundNo("CRF202606060001");
        order.setRefundAmount(3300L);
        order.setStatus(status);
        return order;
    }

    private PaymentApplication application() {
        PaymentApplication application = new PaymentApplication();
        application.setTenantId(1L);
        application.setAppId("app_openapi");
        application.setAppSecret("openapi-secret");
        application.setStatus(1);
        return application;
    }

    private PaymentBusinessOrderEntity businessOrder() {
        PaymentBusinessOrderEntity order = new PaymentBusinessOrderEntity();
        order.setId(360001L);
        order.setTenantId(1L);
        order.setAppCode("app_openapi");
        order.setBizOrderNo("BO202606060001");
        order.setNotifyUrl("https://business.example.test/payment/notify");
        return order;
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
