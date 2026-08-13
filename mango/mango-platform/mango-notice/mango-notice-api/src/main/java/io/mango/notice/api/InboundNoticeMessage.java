package io.mango.notice.api;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeInboundProtocol;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Standardized message produced by inbound channel adapters. */
public record InboundNoticeMessage(
        String tenantId,
        Long channelConfigId,
        NoticeChannelType channelType,
        String providerCode,
        NoticeInboundProtocol protocol,
        String sourceKey,
        String messageId,
        String subject,
        String fromAddress,
        List<String> toAddresses,
        String bodyText,
        String bodyHtml,
        Map<String, String> headers,
        List<InboundNoticeAttachment> attachments,
        Instant receivedAt) {

    public InboundNoticeMessage {
        toAddresses = toAddresses == null ? List.of() : List.copyOf(toAddresses);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }
}
