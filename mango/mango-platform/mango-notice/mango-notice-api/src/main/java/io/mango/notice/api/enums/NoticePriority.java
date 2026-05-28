package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知优先级")
public enum NoticePriority {
    @Schema(description = "低")
    LOW,
    @Schema(description = "普通")
    NORMAL,
    @Schema(description = "高")
    HIGH,
    @Schema(description = "紧急")
    URGENT
}
