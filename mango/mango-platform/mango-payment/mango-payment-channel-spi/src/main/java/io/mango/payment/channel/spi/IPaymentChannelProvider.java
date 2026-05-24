package io.mango.payment.channel.spi;

import io.mango.payment.channel.spi.model.PaymentChannelResponse;
import io.mango.payment.channel.spi.model.PaymentChannelStatus;

public interface IPaymentChannelProvider {

    String channelCode();

    PaymentChannelResponse pay(Long paymentOrderId, Long amount, String subject);

    PaymentChannelStatus queryPayment(Long paymentOrderId, String channelOrderNo);

    boolean closePayment(Long paymentOrderId, String channelOrderNo);
}
