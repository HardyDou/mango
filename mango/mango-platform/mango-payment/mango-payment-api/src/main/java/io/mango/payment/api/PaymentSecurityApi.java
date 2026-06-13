package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.vo.PaymentSensitiveFieldReencryptResultVO;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentSecurityApi {

    R<PaymentSensitiveFieldReencryptResultVO> reencryptSensitiveFields(Integer limit);
}
