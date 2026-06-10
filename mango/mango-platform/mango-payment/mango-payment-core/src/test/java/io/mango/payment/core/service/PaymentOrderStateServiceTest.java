package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentOrderStateServiceTest {

    private final PaymentOrderStateService service = new PaymentOrderStateService();

    @Test
    @DisplayName("business order state machine should allow only documented transitions")
    void requireBusinessTransition_documentedTransitions() {
        service.requireBusinessTransition("TO_PAY", "PAYING");
        service.requireBusinessTransition("PAYING", "PAID");
        service.requireBusinessTransition("SUCCESS", "REFUNDING");
        service.requireBusinessTransition("REFUNDING", "PAID");
        service.requireBusinessTransition("PARTIAL_REFUNDED", "REFUNDED");

        assertThatThrownBy(() -> service.requireBusinessTransition("CLOSED", "PAYING"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("业务订单状态不允许");
        assertThatThrownBy(() -> service.requireBusinessTransition("REFUNDED", "PAYING"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("业务订单状态不允许");
    }

    @Test
    @DisplayName("payment order state machine should reject terminal state rollback")
    void requirePaymentTransition_terminalRollbackRejected() {
        service.requirePaymentTransition("CREATED", "PAYING");
        service.requirePaymentTransition("PAYING", "SUCCESS");
        service.requirePaymentTransition("PAYING", "FAILED");
        service.requirePaymentTransition("SUCCESS", "DUPLICATE_REFUNDING");

        assertThatThrownBy(() -> service.requirePaymentTransition("SUCCESS", "FAILED"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("支付订单状态不允许");
        assertThatThrownBy(() -> service.requirePaymentTransition("FAILED", "SUCCESS"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("支付订单状态不允许");
    }

    @Test
    @DisplayName("new payment result status should be reachable from created state")
    void requireNewPaymentResultStatus_reachableFromCreated() {
        service.requireNewPaymentResultStatus("PAYING");
        service.requireNewPaymentResultStatus("SUCCESS");
        service.requireNewPaymentResultStatus("FAILED");
        service.requireNewPaymentResultStatus("CLOSED");

        assertThatThrownBy(() -> service.requireNewPaymentResultStatus("DUPLICATE_REFUNDED"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("支付订单状态不允许");
    }

    @Test
    @DisplayName("refund order state machine should allow failed retry and reject terminal rollback")
    void requireRefundTransition_documentedTransitions() {
        service.requireRefundTransition("CREATED", "REFUNDING");
        service.requireRefundTransition("REFUNDING", "SUCCESS");
        service.requireRefundTransition("REFUNDING", "FAILED");
        service.requireRefundTransition("FAILED", "REFUNDING");
        service.requireRefundTransition("PROCESSING", "SUCCESS");

        assertThatThrownBy(() -> service.requireRefundTransition("SUCCESS", "FAILED"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("退款订单状态不允许");
        assertThatThrownBy(() -> service.requireRefundTransition("CLOSED", "REFUNDING"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("退款订单状态不允许");
    }

    @Test
    @DisplayName("business order payable and refundable checks should follow design status semantics")
    void payableAndRefundableChecks_followDesignStatus() {
        service.requireBusinessOrderPayable("TO_PAY", LocalDateTime.now().plusMinutes(1));
        service.requireBusinessOrderPayable("PAYING", null);
        service.requireBusinessOrderRefundable("PAID");
        service.requireBusinessOrderRefundable("SUCCESS");
        service.requireBusinessOrderRefundable("PARTIAL_REFUNDED");

        assertThatThrownBy(() -> service.requireBusinessOrderPayable("CLOSED", null))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.requireBusinessOrderPayable("PAYING", LocalDateTime.now().minusSeconds(1)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("业务订单已过期");
        assertThatThrownBy(() -> service.requireBusinessOrderRefundable("PAYING"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("业务订单当前不可退款");
    }

    @Test
    @DisplayName("refund amount should be positive and not exceed remaining refundable cents")
    void requireRefundAmount_validatesRemainingAmount() {
        service.requireRefundAmount(3000L, 8800L, 5000L);

        assertThatThrownBy(() -> service.requireRefundAmount(0L, 8800L, 0L))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.requireRefundAmount(3900L, 8800L, 5000L))
                .isInstanceOf(BizException.class)
                .hasMessage("退款金额超过可退金额");
        assertThatThrownBy(() -> service.requireRefundAmount(100L, 8800L, 9000L))
                .isInstanceOf(BizException.class)
                .hasMessage("金额不能小于 0 分");
    }

    @Test
    @DisplayName("next business status after refund should distinguish partial and full refund")
    void nextBusinessStatusAfterRefund_returnsPartialOrRefunded() {
        assertThat(service.nextBusinessStatusAfterRefund(8800L, 0L, 3300L)).isEqualTo("PARTIAL_REFUNDED");
        assertThat(service.nextBusinessStatusAfterRefund(8800L, 3300L, 5500L)).isEqualTo("REFUNDED");
    }
}
