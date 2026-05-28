package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知发送状态")
public enum NoticeSendStatus {
    @Schema(description = "待发送")
    PENDING,

    @Schema(description = "发送中")
    SENDING,

    @Schema(description = "成功")
    SUCCESS,

    @Schema(description = "失败")
    FAILED,

    @Schema(description = "等待重试")
    RETRY_WAITING,

    @Schema(description = "最终失败")
    FINAL_FAILED,

    @Schema(description = "人工成功")
    MANUAL_SUCCESS,

    @Schema(description = "已忽略")
    IGNORED,

    @Schema(description = "已取消")
    CANCELED
}
