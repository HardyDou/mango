package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新成员组织关系命令。
 */
@Data
@Schema(description = "更新成员组织关系命令")
public class UpdateTenantMemberOrgCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "关系ID不能为空")
    @Positive(message = "关系ID必须大于0")
    @Schema(description = "关系ID")
    private Long relationId;

    @Positive(message = "岗位ID必须大于0")
    @Schema(description = "岗位ID")
    private Long postId;

    @NotNull(message = "主组织标识不能为空")
    @Schema(description = "是否主组织")
    private Boolean primaryFlag;

    @NotNull(message = "组织主管标识不能为空")
    @Schema(description = "是否组织主管")
    private Boolean leaderFlag;

    @NotNull(message = "操作用户ID不能为空")
    @Positive(message = "操作用户ID必须大于0")
    @Schema(description = "操作用户ID")
    private Long operatorUserId;
}
