package io.mango.org.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 组织成员关系更新命令。
 */
@Data
@Schema(description = "修改组织成员关系命令")
public class UpdateOrgMemberCommand {

    @Schema(description = "组织成员关系ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "组织成员关系ID不能为空")
    @Positive(message = "组织成员关系ID必须大于0")
    private Long relationId;

    @Schema(description = "岗位ID")
    @Positive(message = "岗位ID必须大于0")
    private Long postId;

    @Schema(description = "是否主组织", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主组织标志不能为空")
    private Boolean primaryFlag;

    @Schema(description = "是否组织主管", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "组织主管标志不能为空")
    private Boolean leaderFlag;

}
