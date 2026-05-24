package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.command.SandboxPaymentCommand;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.SandboxPaymentNotifyVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface SandboxPaymentApi {

    R<SandboxPaymentNotifyVO> createPaymentNotify(@Valid SandboxPaymentCommand command);

    R<PaymentOrderVO> completePayment(@Valid SandboxPaymentCommand command);
}
