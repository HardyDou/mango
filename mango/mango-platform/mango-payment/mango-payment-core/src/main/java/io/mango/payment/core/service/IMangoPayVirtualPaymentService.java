package io.mango.payment.core.service;

public interface IMangoPayVirtualPaymentService {
    io.mango.payment.api.vo.MangoPayVirtualPaymentResultVO pay(
            io.mango.payment.api.command.MangoPayVirtualPaymentCommand command);
}
