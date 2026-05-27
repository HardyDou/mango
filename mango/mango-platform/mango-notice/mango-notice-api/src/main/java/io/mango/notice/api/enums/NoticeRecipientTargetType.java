package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通知接收目标类型。
 */
@Schema(description = "通知接收目标类型")
public enum NoticeRecipientTargetType {

    /** 用户。 */
    USER,

    /** 组织部门。 */
    ORG,

    /** 岗位。 */
    POST,

    /** 角色。 */
    ROLE
}
