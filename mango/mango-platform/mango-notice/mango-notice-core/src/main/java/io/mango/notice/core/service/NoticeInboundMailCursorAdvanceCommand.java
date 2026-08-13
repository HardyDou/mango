package io.mango.notice.core.service;

import io.mango.notice.api.enums.NoticeInboundProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class NoticeInboundMailCursorAdvanceCommand {
    @Schema(description = "渠道配置 ID") @NotNull private final Long channelConfigId;
    @Schema(description = "接收协议") @NotNull private final NoticeInboundProtocol protocol;
    @Schema(description = "游标值") @Size(max = 500) private final String cursorValue;
    @Schema(description = "游标版本") @Size(max = 200) private final String cursorVersion;
    @Schema(description = "下次轮询时间") @NotNull private final LocalDateTime nextPollAt;
    public NoticeInboundMailCursorAdvanceCommand(Long channelConfigId, NoticeInboundProtocol protocol,
            String cursorValue, String cursorVersion, LocalDateTime nextPollAt) {
        this.channelConfigId = channelConfigId; this.protocol = protocol; this.cursorValue = cursorValue;
        this.cursorVersion = cursorVersion; this.nextPollAt = nextPollAt;
    }
    public Long channelConfigId() { return channelConfigId; }
    public NoticeInboundProtocol protocol() { return protocol; }
    public String cursorValue() { return cursorValue; }
    public String cursorVersion() { return cursorVersion; }
    public LocalDateTime nextPollAt() { return nextPollAt; }
}
