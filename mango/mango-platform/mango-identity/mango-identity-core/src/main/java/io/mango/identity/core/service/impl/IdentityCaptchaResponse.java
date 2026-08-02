package io.mango.identity.core.service.impl;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.identity.api.enums.IdentityCode;
import org.springframework.util.StringUtils;

/**
 * 隔离验证码传输响应与身份域服务。
 */
final class IdentityCaptchaResponse {

    private IdentityCaptchaResponse() {
    }

    static String requireSentKey(R<String> response) {
        R<String> required = Require.nonNull(response, IdentityCode.CONTACT_CAPTCHA_UNAVAILABLE);
        Require.isTrue(required.isSuccess() && StringUtils.hasText(required.getData()),
                IdentityCode.CONTACT_CAPTCHA_UNAVAILABLE, "验证码发送失败");
        return required.getData();
    }

    static void requireVerified(R<Boolean> response) {
        R<Boolean> required = Require.nonNull(response, IdentityCode.CONTACT_CAPTCHA_INVALID);
        Require.isTrue(required.isSuccess() && Boolean.TRUE.equals(required.getData()),
                IdentityCode.CONTACT_CAPTCHA_INVALID);
    }
}
