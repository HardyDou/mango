package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentRefundCompletionDeduplicator {

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentOrderStatePolicy orderStateService;
    private final PaymentOrderStatusFlowRecorder statusFlowService;

    public boolean completeIfDuplicateRefund(
            String tenantId,
            PaymentRefundOrderVO refundOrder,
            String triggerSource,
            String triggerNo,
            LocalDateTime eventTime) {
        Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        String bizRefundNo = PaymentContextSupport.trimToNull(refundOrder.getBizRefundNo());
        if (bizRefundNo == null || !bizRefundNo.startsWith("DUP-")) {
            return false;
        }
        Require.notNull(refundOrder.getPaymentOrderId(), PaymentCode.PAYMENT_ORDER_NOT_FOUND, "重复支付退款缺少原支付订单 ID");
        orderStateService.requirePaymentTransition(
                PaymentOrderStatusEnum.DUPLICATE_REFUNDING.getCode(),
                PaymentOrderStatusEnum.DUPLICATE_REFUNDED.getCode());
        int updated = paymentOrderMapper.markDuplicatePaymentRefunded(tenantId, refundOrder.getPaymentOrderId());
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID, "重复支付退款完成状态已变化，请刷新后重试");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowRecorder.ORDER_TYPE_PAYMENT,
                refundOrder.getPaymentOrderId(),
                refundOrder.getPayOrderNo(),
                PaymentOrderStatusEnum.DUPLICATE_REFUNDING.getCode(),
                PaymentOrderStatusEnum.DUPLICATE_REFUNDED.getCode(),
                triggerSource,
                triggerNo,
                eventTime,
                "重复成功支付退款结果确认后推进支付订单状态");
        return true;
    }
}
