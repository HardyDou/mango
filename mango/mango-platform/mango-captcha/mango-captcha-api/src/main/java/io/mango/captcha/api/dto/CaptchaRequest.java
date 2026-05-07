package io.mango.captcha.api.dto;

import io.mango.captcha.api.constant.CaptchaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码请求
 *
 * @author Mango
 */
@Data
@Schema(description = "验证码生成请求")
public class CaptchaRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码类型
     */
    @Schema(description = "验证码类型")
    @NotNull(message = "验证码类型不能为空")
    private CaptchaType type;

    /**
     * 目标（手机号/邮箱）
     */
    @Schema(description = "目标，例如手机号或邮箱")
    private String target;

    /**
     * 额外参数（用于滑块验证）
     */
    @Schema(description = "额外参数")
    private String extra;
}
