package io.mango.payment.core.service;

public interface IPaymentChannelCallbackService {
    io.mango.payment.api.vo.PaymentChannelCallbackResultVO handle(
            io.mango.payment.api.command.PaymentChannelCallbackCommand command);
}
