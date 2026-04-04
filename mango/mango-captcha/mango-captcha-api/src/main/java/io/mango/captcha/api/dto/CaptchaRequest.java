package io.mango.captcha.api.dto;

import io.mango.captcha.api.constant.CaptchaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码请求
 *
 * @author Mango
 */
@Data
public class CaptchaRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码类型
     */
    @NotNull(message = "验证码类型不能为空")
    private CaptchaType type;

    /**
     * 目标（手机号/邮箱）
     */
    private String target;

    /**
     * 额外参数（用于滑块验证）
     */
    private String extra;
}
