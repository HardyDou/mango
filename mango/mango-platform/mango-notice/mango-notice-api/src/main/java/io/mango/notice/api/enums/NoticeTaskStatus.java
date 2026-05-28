package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知任务状态")
public enum NoticeTaskStatus {
    @Schema(description = "等待发送")
    WAITING,

    @Schema(description = "发送中")
    SENDING,

    @Schema(description = "部分成功")
    PARTIAL_SUCCESS,

    @Schema(description = "全部成功")
    SUCCESS,

    @Schema(description = "失败")
    FAILED,

    @Schema(description = "已取消")
    CANCELED
}
