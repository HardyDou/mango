package io.mango.infra.realtime.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Infrastructure message envelope delivered to connected clients.
 */
public record RealtimeMessage(
        String id,
        String type,
        String content,
        String tenantId,
        Long userId,
        Map<String, Object> headers,
        Instant createdAt) {

    public RealtimeMessage {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        type = type == null || type.isBlank() ? "message" : type;
        tenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static RealtimeMessage of(String type, String content) {
        return new RealtimeMessage(null, type, content, null, null, Map.of(), null);
    }

    public static RealtimeMessage toUser(Long userId, String type, String content) {
        return new RealtimeMessage(null, type, content, null, userId, Map.of(), null);
    }

    public static RealtimeMessage toTenant(String tenantId, String type, String content) {
        return new RealtimeMessage(null, type, content, tenantId, null, Map.of(), null);
    }
}
