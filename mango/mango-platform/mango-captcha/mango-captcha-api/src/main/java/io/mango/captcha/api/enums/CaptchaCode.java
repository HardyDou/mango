package io.mango.captcha.api.enums;

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
    CAPTCHA_INVALID(2409, "验证码校验失败"),

    /**
     * 验证码发送失败。
     *
     * <p>用于发送链路（无可用供应商、供应商返回失败、供应商配置或凭证缺失、第三方短信接口报错），
     * 与 {@link #CAPTCHA_INVALID} 的校验失败语义相互独立，避免把发送失败误当成验证码错误。</p>
     */
    CAPTCHA_SEND_FAILED(2410, "验证码发送失败");

    private final int code;
    private final String message;
}
