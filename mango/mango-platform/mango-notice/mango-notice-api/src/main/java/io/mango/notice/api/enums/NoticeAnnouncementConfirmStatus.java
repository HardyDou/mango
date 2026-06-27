package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "公告确认状态")
public enum NoticeAnnouncementConfirmStatus {

    @Schema(description = "无需确认")
    NOT_REQUIRED,

    @Schema(description = "待确认")
    PENDING,

    @Schema(description = "已确认")
    CONFIRMED
}
