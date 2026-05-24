package io.mango.payment.channel.spi;

public interface IPaymentNotifyVerifier {

    String channelCode();

    boolean verifyPayment(Long paymentOrderId, String channelOrderNo, String notifyEventId, String signature);
}
