package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.core.model.Money;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentOrderStateService {

    private static final String LEGACY_BUSINESS_SUCCESS = "SUCCESS";

    private static final Map<String, Set<String>> BUSINESS_TRANSITIONS = Map.of(
            PaymentBusinessOrderStatusEnum.TO_PAY.getCode(), Set.of(
                    PaymentBusinessOrderStatusEnum.PAYING.getCode(),
                    PaymentBusinessOrderStatusEnum.CLOSED.getCode()),
            PaymentBusinessOrderStatusEnum.PAYING.getCode(), Set.of(
                    PaymentBusinessOrderStatusEnum.PAID.getCode(),
                    PaymentBusinessOrderStatusEnum.CLOSED.getCode()),
            PaymentBusinessOrderStatusEnum.PAID.getCode(), Set.of(
                    PaymentBusinessOrderStatusEnum.REFUNDING.getCode(),
                    PaymentBusinessOrderStatusEnum.PARTIAL_REFUNDED.getCode(),
                    PaymentBusinessOrderStatusEnum.REFUNDED.getCode()),
            PaymentBusinessOrderStatusEnum.REFUNDING.getCode(), Set.of(
                    PaymentBusinessOrderStatusEnum.PARTIAL_REFUNDED.getCode(),
                    PaymentBusinessOrderStatusEnum.REFUNDED.getCode(),
                    PaymentBusinessOrderStatusEnum.PAID.getCode()),
            PaymentBusinessOrderStatusEnum.PARTIAL_REFUNDED.getCode(), Set.of(
                    PaymentBusinessOrderStatusEnum.REFUNDING.getCode(),
                    PaymentBusinessOrderStatusEnum.REFUNDED.getCode()));

    private static final Map<String, Set<String>> PAYMENT_TRANSITIONS = Map.of(
            PaymentOrderStatusEnum.CREATED.getCode(), Set.of(
                    PaymentOrderStatusEnum.PAYING.getCode(),
                    PaymentOrderStatusEnum.CLOSED.getCode()),
            PaymentOrderStatusEnum.PAYING.getCode(), Set.of(
                    PaymentOrderStatusEnum.SUCCESS.getCode(),
                    PaymentOrderStatusEnum.FAILED.getCode(),
                    PaymentOrderStatusEnum.CLOSED.getCode()),
            PaymentOrderStatusEnum.SUCCESS.getCode(), Set.of(
                    PaymentOrderStatusEnum.DUPLICATE_REFUNDING.getCode(),
                    PaymentOrderStatusEnum.DUPLICATE_REFUNDED.getCode()),
            PaymentOrderStatusEnum.DUPLICATE_REFUNDING.getCode(), Set.of(
                    PaymentOrderStatusEnum.DUPLICATE_REFUNDED.getCode(),
                    PaymentOrderStatusEnum.SUCCESS.getCode()));

    private static final Map<String, Set<String>> REFUND_TRANSITIONS = Map.of(
            PaymentRefundOrderStatusEnum.CREATED.getCode(), Set.of(
                    PaymentRefundOrderStatusEnum.REFUNDING.getCode(),
                    PaymentRefundOrderStatusEnum.CLOSED.getCode()),
            PaymentRefundOrderStatusEnum.REFUNDING.getCode(), Set.of(
                    PaymentRefundOrderStatusEnum.SUCCESS.getCode(),
                    PaymentRefundOrderStatusEnum.FAILED.getCode()),
            PaymentRefundOrderStatusEnum.FAILED.getCode(), Set.of(
                    PaymentRefundOrderStatusEnum.REFUNDING.getCode(),
                    PaymentRefundOrderStatusEnum.CLOSED.getCode()));

    public void requireBusinessOrderPayable(String status, LocalDateTime expireTime) {
        String normalized = normalizeBusinessStatus(status);
        Require.isTrue(
                PaymentBusinessOrderStatusEnum.TO_PAY.getCode().equals(normalized)
                        || PaymentBusinessOrderStatusEnum.PAYING.getCode().equals(normalized),
                PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_PAYABLE);
        Require.isTrue(expireTime == null || expireTime.isAfter(LocalDateTime.now()),
                PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_PAYABLE.getCode(), "业务订单已过期");
    }

    public void requireBusinessOrderRefundable(String status) {
        String normalized = normalizeBusinessStatus(status);
        Require.isTrue(
                PaymentBusinessOrderStatusEnum.PAID.getCode().equals(normalized)
                        || PaymentBusinessOrderStatusEnum.PARTIAL_REFUNDED.getCode().equals(normalized),
                PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "业务订单当前不可退款");
    }

    public void requireBusinessTransition(String fromStatus, String toStatus) {
        String normalizedFrom = normalizeBusinessStatus(fromStatus);
        String normalizedTo = normalizeBusinessStatus(toStatus);
        Require.isTrue(canTransit(BUSINESS_TRANSITIONS, normalizedFrom, normalizedTo),
                PaymentCode.PAYMENT_BUSINESS_ORDER_STATE_CHANGED.getCode(),
                "业务订单状态不允许从 " + fromStatus + " 流转到 " + toStatus);
    }

    public void requireNewPaymentResultStatus(String status) {
        String normalized = normalizePaymentStatus(status);
        if (PaymentOrderStatusEnum.PAYING.getCode().equals(normalized)
                || PaymentOrderStatusEnum.CLOSED.getCode().equals(normalized)) {
            requirePaymentTransition(PaymentOrderStatusEnum.CREATED.getCode(), normalized);
            return;
        }
        requirePaymentTransition(PaymentOrderStatusEnum.CREATED.getCode(), PaymentOrderStatusEnum.PAYING.getCode());
        requirePaymentTransition(PaymentOrderStatusEnum.PAYING.getCode(), normalized);
    }

    public void requirePaymentTransition(String fromStatus, String toStatus) {
        String normalizedFrom = normalizePaymentStatus(fromStatus);
        String normalizedTo = normalizePaymentStatus(toStatus);
        Require.isTrue(canTransit(PAYMENT_TRANSITIONS, normalizedFrom, normalizedTo),
                PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(),
                "支付订单状态不允许从 " + fromStatus + " 流转到 " + toStatus);
    }

    public void requireNewRefundResultStatus(String status) {
        String normalized = normalizeRefundStatus(status);
        if (PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(normalized)
                || PaymentRefundOrderStatusEnum.CLOSED.getCode().equals(normalized)) {
            requireRefundTransition(PaymentRefundOrderStatusEnum.CREATED.getCode(), normalized);
            return;
        }
        requireRefundTransition(PaymentRefundOrderStatusEnum.CREATED.getCode(), PaymentRefundOrderStatusEnum.REFUNDING.getCode());
        requireRefundTransition(PaymentRefundOrderStatusEnum.REFUNDING.getCode(), normalized);
    }

    public void requireRefundTransition(String fromStatus, String toStatus) {
        String normalizedFrom = normalizeRefundStatus(fromStatus);
        String normalizedTo = normalizeRefundStatus(toStatus);
        Require.isTrue(canTransit(REFUND_TRANSITIONS, normalizedFrom, normalizedTo),
                PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(),
                "退款订单状态不允许从 " + fromStatus + " 流转到 " + toStatus);
    }

    public void requireRefundAmount(Long refundAmount, Long paidAmount, Long refundedAmount) {
        Require.notNull(refundAmount, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "退款金额不能为空");
        long normalizedRefundAmount = Money.cents(refundAmount).toPositiveCents("退款金额");
        long normalizedPaidAmount = Money.cents(paidAmount == null ? 0L : paidAmount).toPositiveCents("原支付金额");
        long normalizedRefundedAmount = Money.cents(refundedAmount == null ? 0L : refundedAmount).toNonNegativeCents();
        long refundableAmount = Money.cents(normalizedPaidAmount)
                .subtract(Money.cents(normalizedRefundedAmount))
                .toNonNegativeCents();
        Require.isTrue(normalizedRefundAmount <= refundableAmount, PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
    }

    public String nextBusinessStatusAfterRefund(Long paidAmount, Long refundedAmount, Long refundAmount) {
        requireRefundAmount(refundAmount, paidAmount, refundedAmount);
        long nextRefundedAmount = Money.cents(refundedAmount == null ? 0L : refundedAmount)
                .add(Money.cents(refundAmount))
                .toNonNegativeCents();
        long normalizedPaidAmount = Money.cents(paidAmount).toPositiveCents("原支付金额");
        if (nextRefundedAmount >= normalizedPaidAmount) {
            return PaymentBusinessOrderStatusEnum.REFUNDED.getCode();
        }
        return PaymentBusinessOrderStatusEnum.PARTIAL_REFUNDED.getCode();
    }

    private boolean canTransit(Map<String, Set<String>> transitions, String fromStatus, String toStatus) {
        Require.notBlank(fromStatus, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "原状态不能为空");
        Require.notBlank(toStatus, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "目标状态不能为空");
        return transitions.getOrDefault(fromStatus, Set.of()).contains(toStatus);
    }

    private String normalizeBusinessStatus(String status) {
        if (LEGACY_BUSINESS_SUCCESS.equals(status)) {
            return PaymentBusinessOrderStatusEnum.PAID.getCode();
        }
        return status;
    }

    private String normalizePaymentStatus(String status) {
        return status;
    }

    private String normalizeRefundStatus(String status) {
        if ("PROCESSING".equals(status)) {
            return PaymentRefundOrderStatusEnum.REFUNDING.getCode();
        }
        return status;
    }
}
