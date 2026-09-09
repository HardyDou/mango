package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 当前用户修改密码命令。
 */
@Data
@Schema(description = "当前用户修改密码命令")
public class ChangeCurrentUserPasswordCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "旧密码不能为空")
    @Size(max = 200, message = "旧密码最多200个字符")
    @Schema(description = "当前 Mango 账号旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 200, message = "新密码最多200个字符")
    @Schema(description = "新的 Mango 账号密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
