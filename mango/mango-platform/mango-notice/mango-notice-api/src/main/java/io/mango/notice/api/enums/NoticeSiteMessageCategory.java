package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "系统消息分类")
public enum NoticeSiteMessageCategory {
    @Schema(description = "审批类消息")
    APPROVAL,
    @Schema(description = "系统通知")
    SYSTEM,
    @Schema(description = "业务通知")
    BUSINESS
}
