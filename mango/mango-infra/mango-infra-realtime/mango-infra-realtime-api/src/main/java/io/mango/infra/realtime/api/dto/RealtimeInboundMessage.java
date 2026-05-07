package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Client-to-server realtime message accepted from WebSocket connections.
 */
@Schema(description = "实时上行消息")
public record RealtimeInboundMessage(
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
        @Schema(description = "会话ID")
        String sessionId,
        @Schema(description = "扩展请求头")
        Map<String, Object> headers,
        @Schema(description = "接收时间")
        Instant receivedAt) {

    public RealtimeInboundMessage {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        type = type == null || type.isBlank() ? "message" : type;
        tenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }
}
