package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通知发送取消原因码。
 */
@Schema(description = "通知发送取消原因码")
public enum NoticeSendCancelCode {

    /** 用户关闭该消息。 */
    USER_MESSAGE_DISABLED,

    /** 用户关闭该业务域。 */
    USER_BIZ_GROUP_DISABLED,

    /** 用户关闭该渠道。 */
    USER_CHANNEL_DISABLED,

    /** 缺少接收账户。 */
    RECIPIENT_ACCOUNT_MISSING,

    /** 接收账户未验证。 */
    RECIPIENT_ACCOUNT_UNVERIFIED,

    /** 渠道模板未启用。 */
    CHANNEL_TEMPLATE_DISABLED,

    /** 无可用通道。 */
    CHANNEL_UNAVAILABLE
}
