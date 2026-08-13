package io.mango.notice.support.channel;

import io.mango.notice.api.enums.NoticeInboundProtocol;

/** Resolved mailbox connection settings; secrets must never be logged. */
public record NoticeInboundMailAccount(
        String tenantId,
        Long channelConfigId,
        String configCode,
        String host,
        int port,
        boolean ssl,
        String username,
        String password,
        NoticeInboundProtocol protocol,
        String clientName) {
}
