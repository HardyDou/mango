package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增机构成员账号命令。
 */
@Data
@Schema(description = "新增机构成员账号命令")
public class CreateIdentityUserCommand {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名最多100个字符")
    private String username;

    @Schema(description = "初始密码，不传时使用系统默认初始密码")
    @Size(max = 200, message = "密码最多200个字符")
    private String password;

    @Schema(description = "昵称")
    @Size(max = 100, message = "昵称最多100个字符")
    private String nickname;

    @Schema(description = "登录域，例如 INTERNAL")
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

    @Schema(description = "邮箱")
    @Size(max = 100, message = "邮箱最多100个字符")
    private String email;

    @Schema(description = "手机号")
    @Size(max = 32, message = "手机号最多32个字符")
    private String phone;

    @Schema(description = "头像地址")
    @Size(max = 500, message = "头像地址最多500个字符")
    private String avatar;

    @Schema(description = "成员状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注最多500个字符")
    private String remark;
}
