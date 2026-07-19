package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentSecurityApi;
import io.mango.payment.api.vo.PaymentSensitiveFieldReencryptResultVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentSecurityFeignClient", path = "/payment/security")
public interface PaymentSecurityFeignClient extends PaymentSecurityApi {

    @Override
    @PostMapping("/sensitive-fields/reencrypt")
    R<PaymentSensitiveFieldReencryptResultVO> reencryptSensitiveFields(
            @RequestParam(value = "limit", defaultValue = "100") Integer limit);
}
