package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "系统消息删除状态")
public enum NoticeDeleteStatus {
    @Schema(description = "正常")
    NORMAL,
    @Schema(description = "已删除")
    DELETED
}
