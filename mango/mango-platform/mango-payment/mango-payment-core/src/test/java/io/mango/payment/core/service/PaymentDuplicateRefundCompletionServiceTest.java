package io.mango.payment.core.service;

import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentDuplicateRefundCompletionServiceTest {

    private PaymentOrderMapper paymentOrderMapper;
    private PaymentOrderStatusFlowService statusFlowService;
    private PaymentDuplicateRefundCompletionService service;

    @BeforeEach
    void setUp() {
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        statusFlowService = mock(PaymentOrderStatusFlowService.class);
        service = new PaymentDuplicateRefundCompletionService(
                paymentOrderMapper,
                new PaymentOrderStateService(),
                statusFlowService);
    }

    @Test
    @DisplayName("completeIfDuplicateRefund should advance duplicate payment order after trusted refund success")
    void completeIfDuplicateRefund_duplicateRefund_advancesPaymentOrder() {
        LocalDateTime eventTime = LocalDateTime.now();
        PaymentRefundOrderVO refundOrder = duplicateRefundOrder();
        when(paymentOrderMapper.markDuplicatePaymentRefunded(1L, 370001L)).thenReturn(1);

        boolean result = service.completeIfDuplicateRefund(
                1L,
                refundOrder,
                PaymentOrderStatusFlowService.SOURCE_REFUND_QUERY,
                "RO202606060001",
                eventTime);

        assertThat(result).isTrue();
        verify(paymentOrderMapper).markDuplicatePaymentRefunded(1L, 370001L);
        verify(statusFlowService).record(
                eq(1L),
                eq(PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT),
                eq(370001L),
                eq("PO202606060001"),
                eq("DUPLICATE_REFUNDING"),
                eq("DUPLICATE_REFUNDED"),
                eq(PaymentOrderStatusFlowService.SOURCE_REFUND_QUERY),
                eq("RO202606060001"),
                eq(eventTime),
                eq("重复成功支付退款结果确认后推进支付订单状态"));
    }

    @Test
    @DisplayName("completeIfDuplicateRefund should ignore normal business refund")
    void completeIfDuplicateRefund_normalRefund_ignores() {
        PaymentRefundOrderVO refundOrder = duplicateRefundOrder();
        refundOrder.setBizRefundNo("RF202606060001");

        boolean result = service.completeIfDuplicateRefund(
                1L,
                refundOrder,
                PaymentOrderStatusFlowService.SOURCE_REFUND_QUERY,
                "RO202606060001",
                LocalDateTime.now());

        assertThat(result).isFalse();
        verify(paymentOrderMapper, never()).markDuplicatePaymentRefunded(any(), any());
        verify(statusFlowService, never()).record(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private PaymentRefundOrderVO duplicateRefundOrder() {
        PaymentRefundOrderVO refundOrder = new PaymentRefundOrderVO();
        refundOrder.setId(380001L);
        refundOrder.setRefundOrderNo("RO202606060001");
        refundOrder.setBizRefundNo("DUP-PO202606060001");
        refundOrder.setPaymentOrderId(370001L);
        refundOrder.setPayOrderNo("PO202606060001");
        refundOrder.setRefundAmount(9900L);
        refundOrder.setStatus("SUCCESS");
        return refundOrder;
    }
}
