package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知模板版本状态")
public enum NoticeTemplateVersionStatus {
    @Schema(description = "草稿")
    DRAFT,

    @Schema(description = "当前生效")
    ACTIVE,

    @Schema(description = "历史版本")
    HISTORY
}
