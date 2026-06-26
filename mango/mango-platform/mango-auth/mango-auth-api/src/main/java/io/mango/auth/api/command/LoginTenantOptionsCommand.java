package io.mango.auth.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录机构选项查询命令。
 */
@Data
@Schema(description = "登录机构选项查询命令")
public class LoginTenantOptionsCommand {

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名最多100个字符")
    private String username;

    @Schema(description = "登录域，例如 INTERNAL、CUSTOMER")
    @Size(max = 32, message = "登录域最多32个字符")
    private String realm;

    @Schema(description = "应用编码")
    @Size(max = 64, message = "应用编码最多64个字符")
    private String appCode;
}
