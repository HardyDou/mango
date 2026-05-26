package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知渠道最近发送状态")
public enum NoticeChannelSendHealthStatus {
    @Schema(description = "未发送")
    NONE,

    @Schema(description = "成功")
    SUCCESS,

    @Schema(description = "失败")
    FAILED
}
