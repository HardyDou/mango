package io.mango.org.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 在组织内创建成员账号命令。
 */
@Data
@Schema(description = "在组织内创建成员账号命令")
public class CreateOrgMemberAccountCommand {

    @NotNull(message = "组织ID不能为空")
    @Positive(message = "组织ID必须大于0")
    @Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orgId;

    @Positive(message = "岗位ID必须大于0")
    @Schema(description = "岗位ID")
    private Long postId;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名最多100个字符")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Size(max = 200, message = "密码最多200个字符")
    @Schema(description = "初始密码，为空时使用身份模块默认规则")
    private String password;

    @Size(max = 100, message = "姓名最多100个字符")
    @Schema(description = "姓名")
    private String nickname;

    @Size(max = 100, message = "邮箱最多100个字符")
    @Schema(description = "邮箱")
    private String email;

    @Size(max = 32, message = "手机号最多32个字符")
    @Schema(description = "手机号")
    private String phone;

    @Min(value = 0, message = "成员状态只能为0或1")
    @Max(value = 1, message = "成员状态只能为0或1")
    @Schema(description = "成员状态：0禁用，1启用")
    private Integer status;

    @Size(max = 500, message = "备注最多500个字符")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否设为主组织")
    private Boolean primaryFlag;

    @Schema(description = "是否为组织主管")
    private Boolean leaderFlag;
}
