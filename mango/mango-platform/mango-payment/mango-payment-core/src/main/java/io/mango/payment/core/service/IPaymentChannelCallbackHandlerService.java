package io.mango.payment.core.service;

public interface IPaymentChannelCallbackHandlerService {
    PaymentChannelCallbackHandleResult handle(PaymentChannelRawCallback callback);
}
