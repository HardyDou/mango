package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Unified Realtime Envelope Protocol v1.
 */
@Schema(description = "实时上行消息 Envelope")
public record RealtimeInboundMessage(
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
        @Schema(description = "UTC 时间")
        Instant timestamp,
        @Schema(description = "流式信息")
        RealtimeStream stream) {

    public RealtimeInboundMessage {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        version = version == null || version.isBlank() ? "1.0" : version;
        event = event == null ? RealtimeEvent.of("default", "message") : event;
        source = source == null ? RealtimeSource.server() : source;
        context = context == null ? RealtimeContext.of("default", null) : context;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        payload = payload == null ? RealtimePayload.text("") : payload;
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public String type() {
        return event.name();
    }

    public String eventKey() {
        return event.domain() + "." + event.name();
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

    public String sessionId() {
        return source.sessionId();
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
}
