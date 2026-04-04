package io.mango.captcha.api.dto;

import io.mango.captcha.api.constant.CaptchaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码发送请求
 * 用于内部API调用发送短信/邮件验证码
 *
 * @author Mango
 */
@Data
public class CaptchaSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码类型（SMS或EMAIL）
     */
    @NotNull(message = "验证码类型不能为空")
    private CaptchaType type;

    /**
     * 目标（手机号或邮箱）
     */
    @NotBlank(message = "目标不能为空")
    private String target;

    /**
     * 业务类型：LOGIN, PAYMENT, REGISTER, FORGOT_PASSWORD, CHANGE_MOBILE等
     */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /**
     * 有效期（秒），默认300秒
     */
    private Long expireSeconds = 300L;
}
