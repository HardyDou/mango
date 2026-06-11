package io.mango.payment.core.service;

import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFuiouPayChannelAdapterTest {

    private final PaymentFuiouPayChannelAdapter adapter = new PaymentFuiouPayChannelAdapter(
            null,
            null,
            null,
            null);

    @Test
    @DisplayName("mapPaymentQuery should map fuiou success to payment success")
    void mapPaymentQuery_success() {
        IPaymentChannelAdapter.PaymentQueryResult result = adapter.mapPaymentQuery(Map.of(
                "result_code", "000000",
                "trans_stat", "SUCCESS"));

        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getCode());
        assertThat(result.resultType()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("mapPaymentQuery should keep user paying as paying")
    void mapPaymentQuery_userPaying() {
        IPaymentChannelAdapter.PaymentQueryResult result = adapter.mapPaymentQuery(Map.of(
                "result_code", "000000",
                "trans_stat", "USERPAYING"));

        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.PAYING.getCode());
    }

    @Test
    @DisplayName("mapPaymentQuery should map closed and revoked to closed")
    void mapPaymentQuery_closed() {
        IPaymentChannelAdapter.PaymentQueryResult result = adapter.mapPaymentQuery(Map.of(
                "result_code", "000000",
                "trans_stat", "CLOSED"));

        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.CLOSED.getCode());
    }

    @Test
    @DisplayName("mapPaymentQuery should map failed response to failed")
    void mapPaymentQuery_failed() {
        IPaymentChannelAdapter.PaymentQueryResult result = adapter.mapPaymentQuery(Map.of(
                "result_code", "1010",
                "trans_stat", ""));

        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.FAILED.getCode());
    }
}
