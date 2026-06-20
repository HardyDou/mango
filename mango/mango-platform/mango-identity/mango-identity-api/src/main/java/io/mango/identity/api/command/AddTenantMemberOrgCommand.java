package io.mango.identity.api.command;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增成员组织关系命令。
 */
@Data
public class AddTenantMemberOrgCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID。 */
    private Long tenantId;

    /** 成员 ID。 */
    private Long memberId;

    /** 组织 ID。 */
    private Long orgId;

    /** 岗位 ID。 */
    private Long postId;

    /** 是否主组织。 */
    private Boolean primaryFlag;

    /** 是否组织主管。 */
    private Boolean leaderFlag;

    /** 操作用户 ID。 */
    private Long operatorUserId;
}
