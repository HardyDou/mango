package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "公告发布对象类型")
public enum NoticeAnnouncementTargetType {

    @Schema(description = "全员")
    ALL,

    @Schema(description = "组织")
    ORG,

    @Schema(description = "角色")
    ROLE,

    @Schema(description = "指定用户")
    USER
}
