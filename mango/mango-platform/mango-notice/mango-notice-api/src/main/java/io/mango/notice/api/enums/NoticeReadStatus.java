package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "系统消息已读状态")
public enum NoticeReadStatus {
    @Schema(description = "未读")
    UNREAD,
    @Schema(description = "已读")
    READ
}
