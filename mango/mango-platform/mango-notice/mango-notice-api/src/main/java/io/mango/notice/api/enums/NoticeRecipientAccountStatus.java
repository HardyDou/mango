package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通知接收账户状态。
 */
@Schema(description = "通知接收账户状态")
public enum NoticeRecipientAccountStatus {

    /** 未绑定。 */
    UNBOUND,

    /** 待验证。 */
    PENDING_VERIFY,

    /** 已验证。 */
    VERIFIED,

    /** 已禁用。 */
    DISABLED
}
