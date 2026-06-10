package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.command.MangoPayVirtualPaymentCommand;
import io.mango.payment.api.vo.MangoPayVirtualPaymentResultVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface MangoPayVirtualPaymentApi {

    R<MangoPayVirtualPaymentResultVO> pay(@Valid MangoPayVirtualPaymentCommand command);
}
