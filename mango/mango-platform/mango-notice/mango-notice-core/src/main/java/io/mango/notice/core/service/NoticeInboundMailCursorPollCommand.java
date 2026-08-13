package io.mango.notice.core.service;

import io.mango.notice.api.enums.NoticeInboundProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

/** Cursor state written after a successful mailbox poll. */
@Getter
public class NoticeInboundMailCursorPollCommand {
    @Schema(description = "渠道配置 ID") @NotNull private final Long channelConfigId;
    @Schema(description = "接收协议") @NotNull private final NoticeInboundProtocol protocol;
    @Schema(description = "下次轮询时间") @NotNull private final LocalDateTime nextPollAt;

    public NoticeInboundMailCursorPollCommand(Long channelConfigId, NoticeInboundProtocol protocol,
            LocalDateTime nextPollAt) {
        this.channelConfigId = channelConfigId;
        this.protocol = protocol;
        this.nextPollAt = nextPollAt;
    }
    public Long channelConfigId() {
        return channelConfigId;
    }

    public NoticeInboundProtocol protocol() {
        return protocol;
    }

    public LocalDateTime nextPollAt() {
        return nextPollAt;
    }
}
