package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Unified Realtime Envelope Protocol v1.
 */
@Schema(description = "实时下行消息 Envelope")
public record RealtimeOutboundMessage(
        @Schema(description = "消息ID")
        String id,
        @Schema(description = "协议版本")
        String version,
        @Schema(description = "事件定义")
        RealtimeEvent event,
        @Schema(description = "客户端来源")
        RealtimeSource source,
        @Schema(description = "上下文")
        RealtimeContext context,
        @Schema(description = "投递目标")
        RealtimeTarget target,
        @Schema(description = "业务元数据")
        Map<String, Object> metadata,
        @Schema(description = "业务数据")
        RealtimePayload payload,
        @Schema(description = "ACK 信息")
        RealtimeAck ack,
        @Schema(description = "顺序号")
        Long sequence,
        @Schema(description = "状态")
        RealtimeStatus status,
        @Schema(description = "UTC 时间")
        Instant timestamp,
        @Schema(description = "流式信息")
        RealtimeStream stream) {

    public RealtimeOutboundMessage {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        version = version == null || version.isBlank() ? "1.0" : version;
        event = event == null ? RealtimeEvent.of("default", "message") : event;
        source = source == null ? RealtimeSource.server() : source;
        context = context == null ? RealtimeContext.of("default", null) : context;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        payload = payload == null ? RealtimePayload.text("") : payload;
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public static RealtimeOutboundMessage of(String type, String content) {
        return business(RealtimeEvent.fromLegacyType(type), content, null, null, Map.of());
    }

    public static RealtimeOutboundMessage toUser(Long userId, String type, String content) {
        return business(RealtimeEvent.fromLegacyType(type), content, null, userId, Map.of());
    }

    public static RealtimeOutboundMessage toTenant(String tenantId, String type, String content) {
        return business(RealtimeEvent.fromLegacyType(type), content, tenantId, null, Map.of());
    }

    public static RealtimeOutboundMessage business(RealtimeEvent event,
                                                   String content,
                                                   String tenantId,
                                                   Long userId,
                                                   Map<String, Object> metadata) {
        return new RealtimeOutboundMessage(
                null,
                "1.0",
                event,
                RealtimeSource.server(),
                RealtimeContext.of(tenantId, userId),
                userId == null ? null : RealtimeTarget.user(userId),
                metadata,
                RealtimePayload.text(content),
                null,
                null,
                null,
                null,
                null);
    }

    public static RealtimeOutboundMessage accepted(RealtimeInboundMessage inbound, String message) {
        return new RealtimeOutboundMessage(
                null,
                "1.0",
                RealtimeEvent.of(inbound.event().domain(), "message.accepted"),
                RealtimeSource.server(),
                new RealtimeContext(inbound.context().tenantId(), null, inbound.context().traceId(), inbound.context().requestId()),
                RealtimeTarget.connection(inbound.sessionId()),
                Map.of(),
                RealtimePayload.message(message),
                RealtimeAck.accepted(inbound.id()),
                inbound.sequence() == null ? null : inbound.sequence() + 1,
                RealtimeStatus.success(),
                null,
                null);
    }

    public String type() {
        return event.name();
    }

    public String content() {
        return payload.textValue();
    }

    public String tenantId() {
        return context.tenantId();
    }

    public Long userId() {
        return context.userId();
    }

    public RealtimeTarget resolvedTarget() {
        if (target != null) {
            return target;
        }
        if (context.userId() != null) {
            return RealtimeTarget.user(context.userId());
        }
        if (context.tenantId() != null && !"default".equals(context.tenantId())) {
            return RealtimeTarget.tenant(context.tenantId());
        }
        return RealtimeTarget.broadcast();
    }

    public Map<String, Object> headers() {
        return metadata;
    }

    public Instant createdAt() {
        return timestamp;
    }
}
