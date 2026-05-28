package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通知接收偏好范围类型。
 */
@Schema(description = "通知接收偏好范围类型")
public enum NoticeReceivePreferenceScopeType {

    /** 全局。 */
    GLOBAL,

    /** 业务域。 */
    BIZ_GROUP,

    /** 单消息。 */
    BIZ_TYPE
}
