package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知发送模式")
public enum NoticeSendMode {
 @Schema(description = "立即发送")
 IMMEDIATE,

 @Schema(description = "定时发送")
 SCHEDULED
}
