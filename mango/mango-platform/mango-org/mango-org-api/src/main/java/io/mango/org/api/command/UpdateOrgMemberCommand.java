package io.mango.org.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 组织成员关系更新命令。
 */
@Data
public class UpdateOrgMemberCommand {

    /** 关系 ID。 */
    @NotNull(message = "组织成员关系ID不能为空")
    private Long relationId;

    /** 岗位 ID。 */
    private Long postId;

    /** 是否主组织。 */
    private Boolean primaryFlag;

    /** 是否组织主管。 */
    private Boolean leaderFlag;

}
