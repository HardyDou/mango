package io.mango.payment.core.service;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentDuplicatePaymentGuardTest {

    private PaymentOrderMapper paymentOrderMapper;
    private PaymentRefundOrderMapper refundOrderMapper;
    private PaymentTransactionFlowMapper transactionFlowMapper;
    private PaymentExceptionOrderRecorder exceptionOrderRecordService;
    private PaymentOrderStatusFlowRecorder statusFlowService;
    private PaymentNumberGenerator numberService;
    private PaymentDuplicatePaymentGuard service;

    @BeforeEach
    void setUp() {
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        refundOrderMapper = mock(PaymentRefundOrderMapper.class);
        transactionFlowMapper = mock(PaymentTransactionFlowMapper.class);
        exceptionOrderRecordService = mock(PaymentExceptionOrderRecorder.class);
        statusFlowService = mock(PaymentOrderStatusFlowRecorder.class);
        numberService = mock(PaymentNumberGenerator.class);
        when(numberService.next(PaymentNumberGenerator.PAY_FLOW_NO)).thenReturn("PF2026060600000001");
        when(numberService.next(PaymentNumberGenerator.PAY_REFUND_ORDER_NO)).thenReturn("RO2026060600000001");
        service = new PaymentDuplicatePaymentGuard(
                paymentOrderMapper,
                refundOrderMapper,
                transactionFlowMapper,
                exceptionOrderRecordService,
                new PaymentOrderStatePolicy(),
                statusFlowService,
                numberService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("handleDuplicateSuccess should create Mango Pay duplicate refund in refunding status")
    void handleDuplicateSuccess_mangoPay_createsRefundingOrder() {
        LocalDateTime eventTime = LocalDateTime.now();
        PaymentOrderEntity order = paymentOrder("MANGO_PAY");
        when(paymentOrderMapper.markDuplicatePaymentSuccess("1", 370001L, eventTime, "CH202606060001")).thenReturn(1);
        when(paymentOrderMapper.markDuplicatePaymentRefunding("1", 370001L)).thenReturn(1);
        when(refundOrderMapper.selectOne(any())).thenReturn(null);
        ArgumentCaptor<PaymentRefundOrderEntity> refundCaptor = ArgumentCaptor.forClass(PaymentRefundOrderEntity.class);
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);

        PaymentDuplicatePaymentGuard.DuplicatePaymentResult result = service.handleDuplicateSuccess(
                "1",
                order,
                eventTime,
                PaymentOrderStatusFlowRecorder.SOURCE_CHANNEL_CALLBACK,
                "CH202606060001",
                "CH202606060001");

        assertThat(result.status()).isEqualTo("DUPLICATE_REFUNDING");
        assertThat(result.refunded()).isFalse();
        assertThat(result.refundOrderNo()).startsWith("RO");
        assertThat(result.exceptionNo()).isNull();
        verify(refundOrderMapper).insert(refundCaptor.capture());
        PaymentRefundOrderEntity refundOrder = refundCaptor.getValue();
        assertThat(refundOrder.getBizRefundNo()).isEqualTo("DUP-PO202606060001");
        assertThat(refundOrder.getChannelRefundNo()).isEqualTo("DUP-REFUND-PO202606060001");
        assertThat(refundOrder.getRefundAmount()).isEqualTo(9900L);
        assertThat(refundOrder.getStatus()).isEqualTo("REFUNDING");
        assertThat(refundOrder.getRefundTime()).isNull();
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getAllValues())
                .extracting(PaymentTransactionFlowEntity::getFlowType)
                .containsExactly("PAY_SUCCESS");
        verify(paymentOrderMapper, org.mockito.Mockito.never()).markDuplicatePaymentRefunded(any(), any());
        verify(statusFlowService, org.mockito.Mockito.times(3)).record(
                eq("1"), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("handleDuplicateSuccess should create exception order when channel auto refund is not available")
    void handleDuplicateSuccess_externalChannel_createsExceptionOrder() {
        LocalDateTime eventTime = LocalDateTime.now();
        PaymentOrderEntity order = paymentOrder("ALLINPAY");
        when(paymentOrderMapper.markDuplicatePaymentSuccess("1", 370001L, eventTime, "CH202606060001")).thenReturn(1);
        io.mango.payment.core.entity.PaymentExceptionOrderEntity exceptionOrder =
                new io.mango.payment.core.entity.PaymentExceptionOrderEntity();
        exceptionOrder.setExceptionNo("EX2026060600000001");
        when(exceptionOrderRecordService.createIfAbsent(
                eq("1"),
                eq("PO202606060001"),
                eq(PaymentExceptionOrderRecorder.TYPE_DUPLICATE_PAYMENT),
                eq(PaymentExceptionOrderRecorder.SEVERITY_HIGH),
                eq("重复成功支付已落库，当前通道未具备自动退款适配器，已挂起异常处理"),
                eq(eventTime))).thenReturn(exceptionOrder);
        ArgumentCaptor<PaymentTransactionFlowEntity> flowCaptor = ArgumentCaptor.forClass(PaymentTransactionFlowEntity.class);

        PaymentDuplicatePaymentGuard.DuplicatePaymentResult result = service.handleDuplicateSuccess(
                "1",
                order,
                eventTime,
                PaymentOrderStatusFlowRecorder.SOURCE_CHANNEL_CALLBACK,
                "CH202606060001",
                "CH202606060001");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.refunded()).isFalse();
        assertThat(result.refundOrderNo()).isNull();
        assertThat(result.exceptionNo()).isEqualTo("EX2026060600000001");
        verify(exceptionOrderRecordService).createIfAbsent(
                eq("1"),
                eq("PO202606060001"),
                eq(PaymentExceptionOrderRecorder.TYPE_DUPLICATE_PAYMENT),
                eq(PaymentExceptionOrderRecorder.SEVERITY_HIGH),
                eq("重复成功支付已落库，当前通道未具备自动退款适配器，已挂起异常处理"),
                eq(eventTime));
        verify(transactionFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getFlowType()).isEqualTo("PAY_SUCCESS");
        verify(statusFlowService).record(
                eq("1"),
                eq(PaymentOrderStatusFlowRecorder.ORDER_TYPE_PAYMENT),
                eq(370001L),
                eq("PO202606060001"),
                eq("PAYING"),
                eq("SUCCESS"),
                eq(PaymentOrderStatusFlowRecorder.SOURCE_CHANNEL_CALLBACK),
                eq("CH202606060001"),
                eq(eventTime),
                eq("重复成功支付先记录为非有效成功支付"));
    }

    private PaymentOrderEntity paymentOrder(String channelCode) {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(370001L);
        order.setTenantId("1");
        order.setPayOrderNo("PO202606060001");
        order.setBusinessOrderId(360001L);
        order.setChannelCode(channelCode);
        order.setAmount(9900L);
        order.setStatus("PAYING");
        order.setSuccessFlag(0);
        return order;
    }
}
