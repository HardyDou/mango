package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知渠道 Secret 完整性状态")
public enum NoticeChannelSecretStatus {
    NOT_REQUIRED,
    COMPLETE,
    INCOMPLETE
}
