package io.mango.notice.core.service;

import io.mango.notice.api.enums.NoticeInboundProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class NoticeInboundMailCursorFailureCommand {
    @Schema(description = "渠道配置 ID") @NotNull private final Long channelConfigId;
    @Schema(description = "接收协议") @NotNull private final NoticeInboundProtocol protocol;
    @Schema(description = "失败编码") @NotBlank @Size(max = 200) private final String failureCode;
    @Schema(description = "失败原因") @NotBlank @Size(max = 1000) private final String failureReason;
    @Schema(description = "下次轮询时间") @NotNull private final LocalDateTime nextPollAt;
    public NoticeInboundMailCursorFailureCommand(Long channelConfigId, NoticeInboundProtocol protocol,
            String failureCode, String failureReason, LocalDateTime nextPollAt) {
        this.channelConfigId = channelConfigId; this.protocol = protocol; this.failureCode = failureCode;
        this.failureReason = failureReason; this.nextPollAt = nextPollAt;
    }
    public Long channelConfigId() { return channelConfigId; }
    public NoticeInboundProtocol protocol() { return protocol; }
    public String failureCode() { return failureCode; }
    public String failureReason() { return failureReason; }
    public LocalDateTime nextPollAt() { return nextPollAt; }
}
