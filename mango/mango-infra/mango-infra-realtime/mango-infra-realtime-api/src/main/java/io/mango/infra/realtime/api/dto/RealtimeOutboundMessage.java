package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Infrastructure message envelope delivered to connected clients.
 */
@Schema(description = "实时下行消息")
public record RealtimeOutboundMessage(
        @Schema(description = "消息ID，为空时服务端自动生成")
        String id,
        @Schema(description = "消息类型")
        String type,
        @Schema(description = "消息内容")
        String content,
        @Schema(description = "租户ID")
        String tenantId,
        @Schema(description = "用户ID")
        Long userId,
        @Schema(description = "扩展请求头")
        Map<String, Object> headers,
        @Schema(description = "创建时间")
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
