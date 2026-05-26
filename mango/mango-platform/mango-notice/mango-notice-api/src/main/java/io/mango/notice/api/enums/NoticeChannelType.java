package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知渠道")
public enum NoticeChannelType {
    @Schema(description = "站内信")
    SITE,

    @Schema(description = "短信")
    SMS,

    @Schema(description = "邮件")
    EMAIL,

    @Schema(description = "微信公众号")
    WECHAT_OFFICIAL,

    @Schema(description = "企业微信")
    WECOM,

    @Schema(description = "钉钉")
    DINGTALK
}
