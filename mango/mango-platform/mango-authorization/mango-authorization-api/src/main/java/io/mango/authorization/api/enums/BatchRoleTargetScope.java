package io.mango.authorization.api.enums;

import lombok.Getter;

/** 批量角色绑定的成员目标范围。 */
@Getter
public enum BatchRoleTargetScope {
    /** 当前租户全部启用成员。 */
    ALL_ENABLED_MEMBERS,
    /** 指定成员 ID。 */
    SUBJECT_IDS,
    /** 指定组织自身成员。 */
    ORGANIZATION,
    /** 指定组织及全部启用下级组织成员。 */
    ORGANIZATION_AND_DESCENDANTS
}
