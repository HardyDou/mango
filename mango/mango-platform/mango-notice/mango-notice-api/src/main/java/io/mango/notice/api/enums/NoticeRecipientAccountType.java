package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通知接收账户类型。
 */
@Schema(description = "通知接收账户类型")
public enum NoticeRecipientAccountType {

    /** 手机号。 */
    MOBILE,

    /** 邮箱。 */
    EMAIL,

    /** 微信公众号。 */
    WECHAT,

    /** 企业微信。 */
    WECOM,

    /** 钉钉。 */
    DINGTALK,

    /** 飞书。 */
    FEISHU
}
