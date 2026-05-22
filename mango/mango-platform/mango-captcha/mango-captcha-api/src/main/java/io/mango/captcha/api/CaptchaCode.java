package io.mango.captcha.api;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验证码模块业务码。
 */
@Getter
@AllArgsConstructor
public enum CaptchaCode implements BizCode {

    /** 验证码校验失败。 */
    CAPTCHA_INVALID(2409, "验证码校验失败");

    private final int code;
    private final String message;
}
