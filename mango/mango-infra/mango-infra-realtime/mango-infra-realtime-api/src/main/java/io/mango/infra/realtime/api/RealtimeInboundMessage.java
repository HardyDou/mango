package io.mango.infra.realtime.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Client-to-server realtime message accepted from WebSocket connections.
 */
public record RealtimeInboundMessage(
        String id,
        String type,
        String content,
        String tenantId,
        Long userId,
        String sessionId,
        Map<String, Object> headers,
        Instant receivedAt) {

    public RealtimeInboundMessage {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        type = type == null || type.isBlank() ? "message" : type;
        tenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }
}
