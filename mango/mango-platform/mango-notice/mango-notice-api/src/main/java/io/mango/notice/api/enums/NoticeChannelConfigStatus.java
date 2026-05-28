package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知渠道配置状态")
public enum NoticeChannelConfigStatus {
    @Schema(description = "完整")
    COMPLETE,

    @Schema(description = "未完成")
    INCOMPLETE
}
