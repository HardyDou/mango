package io.mango.auth.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 认证入口发送短信或邮件验证码命令。
 */
@Data
@Schema(description = "认证验证码发送命令")
public class SendAuthCaptchaCommand {

    @Schema(description = "验证码类型，例如 SMS、EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "验证码类型不能为空")
    private AuthCaptchaType type;

    @Schema(description = "发送目标，例如手机号或邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标不能为空")
    @Size(max = 320, message = "发送目标最多320个字符")
    private String target;

    @Schema(description = "业务类型，例如 LOGIN、REGISTER、FORGOT_PASSWORD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 64, message = "业务类型最多64个字符")
    private String businessType;

    @Schema(description = "有效期，单位秒，默认300")
    @Positive(message = "有效期必须大于0")
    private Long expireSeconds = 300L;

    public enum AuthCaptchaType {
        SMS,
        EMAIL
    }
}
