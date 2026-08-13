package io.mango.notice.core.service;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** Identifies a broadcast that exhausted its retry budget. */
@Getter
public class NoticeInboundBroadcastDeadLetterCommand {
    @Schema(description = "租户标识") @NotBlank private final String tenantId;
    @Schema(description = "消息 ID") @NotNull private final Long messageId;
    @Schema(description = "失败原因") @NotBlank @Size(max = 1000) private final String reason;

    public NoticeInboundBroadcastDeadLetterCommand(String tenantId, Long messageId, String reason) {
        this.tenantId = tenantId;
        this.messageId = messageId;
        this.reason = reason;
    }
    public String tenantId() { return tenantId; }
    public Long messageId() { return messageId; }
    public String reason() { return reason; }
}
