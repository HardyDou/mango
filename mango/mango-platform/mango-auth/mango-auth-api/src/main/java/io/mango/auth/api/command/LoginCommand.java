package io.mango.auth.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录命令。
 */
@Data
@Schema(description = "登录命令")
public class LoginCommand {

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名最多100个字符")
    private String username;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    @Size(max = 200, message = "密码最多200个字符")
    private String password;

    @Schema(description = "机构ID。登录时必须显式选择机构，tenantId 与 tenantCode 至少传一个")
    @Size(max = 64, message = "机构ID最多64个字符")
    private String tenantId;

    @Schema(description = "机构编码。登录时必须显式选择机构，tenantId 与 tenantCode 至少传一个")
    @Size(max = 50, message = "机构编码最多50个字符")
    private String tenantCode;

    @Schema(description = "登录域，例如 INTERNAL、CUSTOMER")
    @Size(max = 32, message = "登录域最多32个字符")
    private String realm;

    @Schema(description = "操作者类型，例如 INTERNAL_USER")
    @Size(max = 32, message = "操作者类型最多32个字符")
    private String actorType;

    @Schema(description = "归属主体类型，例如 INTERNAL_ORG")
    @Size(max = 64, message = "归属主体类型最多64个字符")
    private String partyType;

    @Schema(description = "归属主体ID")
    private Long partyId;

    @Schema(description = "应用编码")
    @Size(max = 64, message = "应用编码最多64个字符")
    private String appCode;

    @Schema(description = "验证码")
    @Size(max = 64, message = "验证码最多64个字符")
    private String captchaCode;

    @Schema(description = "验证码键")
    @Size(max = 128, message = "验证码键最多128个字符")
    private String captchaKey;
}
