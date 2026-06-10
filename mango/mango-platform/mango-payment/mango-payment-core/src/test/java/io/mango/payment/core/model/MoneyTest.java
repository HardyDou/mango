package io.mango.payment.core.model;

import io.mango.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("money calculation should keep decimal cents and round to non-negative integer cents at the boundary")
    void calculationKeepsPrecisionAndRoundsAtBoundary() {
        Money fee = Money.cents(12_345L).multiply(new BigDecimal("0.006789"));

        assertThat(fee.preciseCents().scale()).isGreaterThanOrEqualTo(Money.MIN_CALCULATION_SCALE);
        assertThat(fee.preciseCents()).isEqualByComparingTo(new BigDecimal("83.81020500"));
        assertThat(fee.toNonNegativeCents()).isEqualTo(84L);
    }

    @Test
    @DisplayName("money should reject negative final cents")
    void negativeFinalCentsRejected() {
        Money amount = Money.preciseCents(new BigDecimal("-0.4000"));

        assertThatThrownBy(amount::toNonNegativeCents)
                .isInstanceOf(BizException.class)
                .hasMessage("金额不能小于 0 分");
    }

    @Test
    @DisplayName("money final cents should be integer cents rounded from high precision calculation")
    void finalCentsRoundedFromHighPrecisionCalculation() {
        Money discount = Money.cents(999L).multiply(new BigDecimal("0.123456789"));

        assertThat(discount.preciseCents().scale()).isGreaterThanOrEqualTo(4);
        assertThat(discount.preciseCents()).isEqualByComparingTo(new BigDecimal("123.333332211"));
        assertThat(discount.toNonNegativeCents()).isEqualTo(123L);
    }

    @Test
    @DisplayName("money should calculate absolute cent difference through value object")
    void absoluteDifferenceUsesMoneyBoundary() {
        long difference = Money.cents(10_000L)
                .subtract(Money.cents(9_900L))
                .abs()
                .toNonNegativeCents();

        assertThat(difference).isEqualTo(100L);
    }

    @Test
    @DisplayName("payment amount should round from high precision and require positive cents")
    void positivePaymentAmountRequired() {
        Money amount = Money.preciseCents(new BigDecimal("0.4900"));
        Money positiveAmount = Money.preciseCents(new BigDecimal("0.5000"));

        assertThatThrownBy(() -> amount.toPositiveCents("付款金额"))
                .isInstanceOf(BizException.class)
                .hasMessage("付款金额必须大于 0 分");
        assertThat(positiveAmount.toPositiveCents("付款金额")).isEqualTo(1L);
    }

    @Test
    @DisplayName("money range should reject negative limits and inverted range")
    void rangeValidationRejectsInvalidAmounts() {
        assertThatThrownBy(() -> Money.requireRange(-1L, 100L, "支付方式"))
                .isInstanceOf(BizException.class)
                .hasMessage("支付方式最小金额不能小于 0 分");

        assertThatThrownBy(() -> Money.requireRange(200L, 100L, "支付方式"))
                .isInstanceOf(BizException.class)
                .hasMessage("支付方式金额范围不正确");
    }

    @Test
    @DisplayName("money should reject amount overflow with business exception")
    void overflowRejectedWithBusinessException() {
        Money amount = Money.preciseCents(new BigDecimal("9223372036854775808.0000"));

        assertThatThrownBy(amount::toNonNegativeCents)
                .isInstanceOf(BizException.class)
                .hasMessage("金额超过系统支持上限");
    }
}
