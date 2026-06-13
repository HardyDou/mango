package io.mango.payment.core.model;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 支付域金额值对象。内部金额单位为分，过程计算保留高精度，最终落库前按业务语义收敛为整数分。
 */
public final class Money implements Comparable<Money> {

    public static final int CALCULATION_SCALE = 8;
    public static final int MIN_CALCULATION_SCALE = 4;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(CALCULATION_SCALE, DEFAULT_ROUNDING);
    private static final BigDecimal MAX_LONG = BigDecimal.valueOf(Long.MAX_VALUE);

    private final BigDecimal centValue;

    private Money(BigDecimal centValue) {
        Require.notNull(centValue, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "金额不能为空");
        this.centValue = normalize(centValue);
    }

    public static Money cents(Long cents) {
        Require.notNull(cents, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "金额不能为空");
        return new Money(BigDecimal.valueOf(cents));
    }

    public static Money preciseCents(BigDecimal cents) {
        return new Money(cents);
    }

    public static void requireNonNegativeCents(Long cents, String fieldName) {
        if (cents == null) {
            return;
        }
        Require.isTrue(cents >= 0, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), fieldName + "不能小于 0 分");
    }

    public static void requireRange(Long minAmount, Long maxAmount, String fieldName) {
        requireNonNegativeCents(minAmount, fieldName + "最小金额");
        requireNonNegativeCents(maxAmount, fieldName + "最大金额");
        if (minAmount != null && maxAmount != null) {
            Require.isTrue(minAmount <= maxAmount, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), fieldName + "金额范围不正确");
        }
    }

    public Money add(Money other) {
        Require.notNull(other, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "金额不能为空");
        return new Money(centValue.add(other.centValue));
    }

    public Money subtract(Money other) {
        Require.notNull(other, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "金额不能为空");
        return new Money(centValue.subtract(other.centValue));
    }

    public Money abs() {
        return centValue.signum() < 0 ? new Money(centValue.abs()) : this;
    }

    public Money multiply(BigDecimal multiplier) {
        Require.notNull(multiplier, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "金额计算比例不能为空");
        return new Money(centValue.multiply(multiplier));
    }

    public BigDecimal preciseCents() {
        return centValue;
    }

    public long toNonNegativeCents() {
        Require.isTrue(centValue.signum() >= 0, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "金额不能小于 0 分");
        BigDecimal rounded = centValue.setScale(0, DEFAULT_ROUNDING);
        Require.isTrue(rounded.compareTo(MAX_LONG) <= 0, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "金额超过系统支持上限");
        return rounded.longValueExact();
    }

    public long toPositiveCents(String fieldName) {
        long cents = toNonNegativeCents();
        Require.isTrue(cents > 0, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), fieldName + "必须大于 0 分");
        return cents;
    }

    public boolean isNegative() {
        return centValue.signum() < 0;
    }

    @Override
    public int compareTo(Money other) {
        return centValue.compareTo(other.centValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Money other)) {
            return false;
        }
        return centValue.compareTo(other.centValue) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(centValue.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return centValue.toPlainString();
    }

    private static BigDecimal normalize(BigDecimal value) {
        int scale = Math.max(Math.max(value.scale(), MIN_CALCULATION_SCALE), CALCULATION_SCALE);
        return value.setScale(scale, DEFAULT_ROUNDING);
    }

    public static Money zero() {
        return new Money(ZERO);
    }
}
