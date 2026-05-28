package io.mango.identity.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 身份用户解析目标类型。
 */
@Schema(description = "身份用户解析目标类型")
public enum IdentityUserTargetType {

    /** 用户。 */
    USER,

    /** 组织部门。 */
    ORG,

    /** 岗位。 */
    POST,

    /** 角色。 */
    ROLE
}
