package io.mango.notice.api;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeInboundProtocol;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Standardized message request produced by inbound channel adapters. */
@Getter
public class InboundNoticeMessageRequest {

    @Schema(description = "租户标识") @NotBlank private final String tenantId;
    @Schema(description = "渠道配置 ID") @NotNull private final Long channelConfigId;
    @Schema(description = "渠道类型") @NotNull private final NoticeChannelType channelType;
    @Schema(description = "提供方编码") @Size(max = 100) private final String providerCode;
    @Schema(description = "接收协议") @NotNull private final NoticeInboundProtocol protocol;
    @Schema(description = "来源幂等键") @NotBlank @Size(max = 500) private final String sourceKey;
    @Schema(description = "来源消息 ID") @Size(max = 500) private final String messageId;
    @Schema(description = "消息主题") @Size(max = 1000) private final String subject;
    @Schema(description = "发件地址") @Size(max = 500) private final String fromAddress;
    @Schema(description = "收件地址") @NotNull @Size(max = 100) private final List<@Size(max = 500) String> toAddresses;
    @Schema(description = "正文文本") @NotNull @Size(max = 2_000_000) private final String bodyText;
    @Schema(description = "正文 HTML") @NotNull @Size(max = 2_000_000) private final String bodyHtml;
    @Schema(description = "消息头") @NotNull @Valid private final List<InboundNoticeHeaderRequest> headers;
    @Schema(description = "附件列表") @Valid @NotNull private final List<InboundNoticeAttachmentRequest> attachments;
    @Schema(description = "接收时间") @NotNull private final Instant receivedAt;

    public String tenantId() { return tenantId; }
    public Long channelConfigId() { return channelConfigId; }
    public NoticeChannelType channelType() { return channelType; }
    public String providerCode() { return providerCode; }
    public NoticeInboundProtocol protocol() { return protocol; }
    public String sourceKey() { return sourceKey; }
    public String messageId() { return messageId; }
    public String subject() { return subject; }
    public String fromAddress() { return fromAddress; }
    public List<String> toAddresses() { return toAddresses; }
    public String bodyText() { return bodyText; }
    public String bodyHtml() { return bodyHtml; }
    public List<InboundNoticeHeaderRequest> headers() { return headers; }
    public List<InboundNoticeAttachmentRequest> attachments() { return attachments; }
    public Instant receivedAt() { return receivedAt; }

    public InboundNoticeMessageRequest(String tenantId, Long channelConfigId, NoticeChannelType channelType,
            String providerCode, NoticeInboundProtocol protocol, String sourceKey, String messageId,
            String subject, String fromAddress, List<String> toAddresses, String bodyText, String bodyHtml,
            List<InboundNoticeHeaderRequest> headers, List<InboundNoticeAttachmentRequest> attachments, Instant receivedAt) {
        this.tenantId = tenantId;
        this.channelConfigId = channelConfigId;
        this.channelType = channelType;
        this.providerCode = providerCode;
        this.protocol = protocol;
        this.sourceKey = sourceKey;
        this.messageId = messageId;
        this.subject = subject;
        this.fromAddress = fromAddress;
        this.toAddresses = toAddresses == null ? List.of() : List.copyOf(toAddresses);
        this.bodyText = bodyText;
        this.bodyHtml = bodyHtml;
        this.headers = headers == null ? List.of() : List.copyOf(headers);
        this.attachments = attachments == null ? List.of() : List.copyOf(attachments);
        this.receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }
}
