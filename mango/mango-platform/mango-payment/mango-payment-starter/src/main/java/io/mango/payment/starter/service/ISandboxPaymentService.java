package io.mango.payment.starter.service;

import io.mango.payment.api.command.SandboxPaymentCommand;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.SandboxPaymentNotifyVO;

public interface ISandboxPaymentService {

    SandboxPaymentNotifyVO createPaymentNotify(SandboxPaymentCommand command);

    PaymentOrderVO completePayment(SandboxPaymentCommand command);
}
