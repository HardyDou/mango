package io.mango.identity.api.command;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新成员组织关系命令。
 */
@Data
public class UpdateTenantMemberOrgCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关系 ID。 */
    private Long relationId;

    /** 岗位 ID。 */
    private Long postId;

    /** 是否主组织。 */
    private Boolean primaryFlag;

    /** 是否组织主管。 */
    private Boolean leaderFlag;

    /** 操作用户 ID。 */
    private Long operatorUserId;
}
