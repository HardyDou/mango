package io.mango.payment.core.service;

public interface IPaymentSensitiveFieldReencryptService {
    io.mango.payment.api.vo.PaymentSensitiveFieldReencryptResultVO reencryptCurrentTenant(Integer limit);
}
