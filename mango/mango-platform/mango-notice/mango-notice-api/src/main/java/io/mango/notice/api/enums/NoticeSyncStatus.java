package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知配置同步状态")
public enum NoticeSyncStatus {
    @Schema(description = "已同步")
    SYNCED,

    @Schema(description = "待发布")
    PENDING_PUBLISH
}
