package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.payment.api.PaymentCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentChannelCallbackHandlerRegistryTest {

    @Test
    @DisplayName("handle should route callback by channel code")
    void handle_routesByChannelCode() {
        AtomicReference<PaymentChannelRawCallback> received = new AtomicReference<>();
        PaymentChannelCallbackHandlerRegistry registry = new PaymentChannelCallbackHandlerRegistry(List.of(new TestHandler(received)));
        PaymentChannelRawCallback callback = new PaymentChannelRawCallback(
                "fuiou_pay",
                "POST",
                "/api/payment/channel-callbacks/fuiou_pay",
                null,
                "application/x-www-form-urlencoded",
                "127.0.0.1",
                Map.of("req", "xml"),
                null,
                LocalDateTime.now());

        PaymentChannelCallbackHandleResult result = registry.handle(callback);

        assertThat(received.get()).isSameAs(callback);
        assertThat(result.responseBody()).isEqualTo("OK");
    }

    @Test
    @DisplayName("handle should fail when callback handler is not registered")
    void handle_unregisteredChannel_fails() {
        PaymentChannelCallbackHandlerRegistry registry = new PaymentChannelCallbackHandlerRegistry(List.of(new TestHandler(new AtomicReference<>())));
        PaymentChannelRawCallback callback = new PaymentChannelRawCallback(
                "UNKNOWN_PAY",
                "POST",
                "/api/payment/channel-callbacks/unknown_pay",
                null,
                "application/x-www-form-urlencoded",
                "127.0.0.1",
                Map.of(),
                null,
                LocalDateTime.now());

        assertThatThrownBy(() -> registry.handle(callback))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode());
    }

    private record TestHandler(AtomicReference<PaymentChannelRawCallback> received) implements IPaymentChannelCallbackHandler {

        @Override
        public String channelCode() {
            return "FUIOU_PAY";
        }

        @Override
        public PaymentChannelCallbackHandleResult handle(PaymentChannelRawCallback callback) {
            received.set(callback);
            return PaymentChannelCallbackHandleResult.text("OK");
        }
    }
}
