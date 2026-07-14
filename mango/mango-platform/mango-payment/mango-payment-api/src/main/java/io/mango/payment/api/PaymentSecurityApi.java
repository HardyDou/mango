package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.vo.PaymentSensitiveFieldReencryptResultVO;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentSecurityApi {

    R<PaymentSensitiveFieldReencryptResultVO> reencryptSensitiveFields(
            @Positive(message = "处理数量必须大于 0") Integer limit);
}
