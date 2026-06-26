package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "公告状态")
public enum NoticeAnnouncementStatus {

    @Schema(description = "草稿")
    DRAFT,

    @Schema(description = "已发布")
    PUBLISHED,

    @Schema(description = "已下线")
    OFFLINE
}
