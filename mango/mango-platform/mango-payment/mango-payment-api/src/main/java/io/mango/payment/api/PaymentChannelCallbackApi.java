package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.vo.PaymentChannelCallbackResultVO;
import jakarta.validation.Valid;

public interface PaymentChannelCallbackApi {

    R<PaymentChannelCallbackResultVO> handle(@Valid PaymentChannelCallbackCommand command);
}
