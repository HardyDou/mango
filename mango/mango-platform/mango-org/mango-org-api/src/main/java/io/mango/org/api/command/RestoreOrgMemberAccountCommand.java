package io.mango.org.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Restore a retained tenant member into an organization. */
@Data
@Schema(description = "恢复原成员到组织命令")
public class RestoreOrgMemberAccountCommand {

    @NotNull(message = "组织ID不能为空")
    @Positive(message = "组织ID必须大于0")
    @Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orgId;

    @Positive(message = "岗位ID必须大于0")
    @Schema(description = "岗位ID")
    private Long postId;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名最多100个字符")
    @Schema(description = "登录用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Size(max = 32, message = "登录域最多32个字符")
    @Schema(description = "登录域")
    private String realm;
}
