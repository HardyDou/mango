package io.mango.org.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 组织成员加入命令。
 */
@Data
@Schema(description = "添加组织成员命令")
public class AddOrgMemberCommand {

    @Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "组织ID不能为空")
    @Positive(message = "组织ID必须大于0")
    private Long orgId;

    @Schema(description = "成员ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "成员ID不能为空")
    @Positive(message = "成员ID必须大于0")
    private Long memberId;

    @Schema(description = "岗位ID")
    @Positive(message = "岗位ID必须大于0")
    private Long postId;

    @Schema(description = "是否设置为主组织", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主组织标志不能为空")
    private Boolean primaryFlag;

    @Schema(description = "是否设置为组织主管", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "组织主管标志不能为空")
    private Boolean leaderFlag;

}
