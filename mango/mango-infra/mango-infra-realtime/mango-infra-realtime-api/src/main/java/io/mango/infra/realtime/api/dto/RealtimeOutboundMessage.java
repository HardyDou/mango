package io.mango.infra.realtime.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Infrastructure message envelope delivered to connected clients.
 */
public record RealtimeOutboundMessage(
        String id,
        String type,
        String content,
        String tenantId,
        Long userId,
        Map<String, Object> headers,
        Instant createdAt) {

    public RealtimeOutboundMessage {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        type = type == null || type.isBlank() ? "message" : type;
        tenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static RealtimeOutboundMessage of(String type, String content) {
        return new RealtimeOutboundMessage(null, type, content, null, null, Map.of(), null);
    }

    public static RealtimeOutboundMessage toUser(Long userId, String type, String content) {
        return new RealtimeOutboundMessage(null, type, content, null, userId, Map.of(), null);
    }

    public static RealtimeOutboundMessage toTenant(String tenantId, String type, String content) {
        return new RealtimeOutboundMessage(null, type, content, tenantId, null, Map.of(), null);
    }
}
