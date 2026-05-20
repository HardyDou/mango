package io.mango.org.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 组织成员加入命令。
 */
@Data
public class AddOrgMemberCommand {

    /** 成员 ID。 */
    @NotNull(message = "成员ID不能为空")
    private Long memberId;

    /** 岗位 ID。 */
    private Long postId;

    /** 是否设置为主组织。 */
    private Boolean primaryFlag;

    /** 是否设置为组织主管。 */
    private Boolean leaderFlag;

}
