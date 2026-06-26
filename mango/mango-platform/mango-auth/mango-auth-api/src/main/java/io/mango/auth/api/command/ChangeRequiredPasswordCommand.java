package io.mango.auth.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录强制改密命令。
 */
@Data
@Schema(description = "登录强制改密命令")
public class ChangeRequiredPasswordCommand {

    @Schema(description = "强制改密凭据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "强制改密凭据不能为空")
    @Size(max = 200, message = "强制改密凭据最多200个字符")
    private String passwordResetTicket;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(max = 200, message = "新密码最多200个字符")
    private String newPassword;

    @Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "确认密码不能为空")
    @Size(max = 200, message = "确认密码最多200个字符")
    private String confirmPassword;
}
