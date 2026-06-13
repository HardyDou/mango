package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.payment.api.PaymentSecurityApi;
import io.mango.payment.api.vo.PaymentSensitiveFieldReencryptResultVO;
import io.mango.payment.core.service.PaymentSensitiveFieldReencryptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/security")
@RequiredArgsConstructor
@Tag(name = "支付安全", description = "支付域敏感字段安全治理接口")
public class PaymentSecurityController implements PaymentSecurityApi {

    private final PaymentSensitiveFieldReencryptService sensitiveFieldReencryptService;

    @Override
    @PostMapping("/sensitive-fields/reencrypt")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:security:reencrypt-sensitive")
    @Operation(summary = "重加密历史敏感字段", description = "对当前租户历史明文应用密钥、主体证件号和银行账号执行受控重加密，只返回处理计数")
    public R<PaymentSensitiveFieldReencryptResultVO> reencryptSensitiveFields(
            @Parameter(description = "本轮最多处理记录数，1-1000") @RequestParam(defaultValue = "100") Integer limit) {
        return R.ok(sensitiveFieldReencryptService.reencryptCurrentTenant(limit));
    }
}
