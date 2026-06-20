package io.mango.identity.api.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 成员组织关系事实。
 */
@Data
public class TenantMemberOrgRelationInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关系 ID。 */
    private Long relationId;

    /** 租户 ID。 */
    private Long tenantId;

    /** 成员 ID。 */
    private Long memberId;

    /** 用户 ID。 */
    private Long userId;

    /** 用户名。 */
    private String username;

    /** 昵称。 */
    private String nickname;

    /** 成员显示名称。 */
    private String displayName;

    /** 成员类型。 */
    private String memberType;

    /** 成员状态。 */
    private Integer status;

    /** 组织 ID。 */
    private Long orgId;

    /** 岗位 ID。 */
    private Long postId;

    /** 是否主组织。 */
    private Boolean primaryFlag;

    /** 是否组织主管。 */
    private Boolean leaderFlag;
}
