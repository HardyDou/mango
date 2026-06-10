package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.payment.api.PaymentCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentChannelAdapterRegistryTest {

    @Test
    @DisplayName("requireAdapter should resolve registered adapter by normalized channel code")
    void requireAdapter_registered_returnsAdapter() {
        IPaymentChannelAdapter adapter = mock(IPaymentChannelAdapter.class);
        when(adapter.channelCode()).thenReturn("MANGO_PAY");
        PaymentChannelAdapterRegistry registry = new PaymentChannelAdapterRegistry(List.of(adapter));

        IPaymentChannelAdapter result = registry.requireAdapter(" mango_pay ");

        assertThat(result).isSameAs(adapter);
    }

    @Test
    @DisplayName("requireAdapter should reject channels without real adapter")
    void requireAdapter_missing_rejects() {
        PaymentChannelAdapterRegistry registry = new PaymentChannelAdapterRegistry(List.of());

        assertThatThrownBy(() -> registry.requireAdapter("ALLINPAY"))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode());
    }
}
