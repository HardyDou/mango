package io.mango.infra.realtime.api.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.common.contract.LocalCapabilityContract;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Unified Realtime Envelope Protocol v1.
 */
@LocalCapabilityContract
@Schema(description = "实时上行消息 Envelope")
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "Metadata is defensively copied to an immutable map by the compact constructor")
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
        id = defaultId(id);
        version = defaultVersion(version);
        event = defaultEvent(event);
        source = defaultSource(source);
        context = defaultContext(context);
        metadata = immutableMetadata(metadata);
        payload = copyPayload(payload);
        timestamp = defaultTimestamp(timestamp);
    }

    @Override
    public RealtimePayload payload() {
        return new RealtimePayload(payload);
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

    private static String defaultId(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value;
    }

    private static String defaultVersion(String value) {
        if (value == null || value.isBlank()) {
            return "1.0";
        }
        return value;
    }

    private static RealtimeEvent defaultEvent(RealtimeEvent value) {
        if (value == null) {
            return RealtimeEvent.of("default", "message");
        }
        return value;
    }

    private static RealtimeSource defaultSource(RealtimeSource value) {
        if (value == null) {
            return RealtimeSource.server();
        }
        return value;
    }

    private static RealtimeContext defaultContext(RealtimeContext value) {
        if (value == null) {
            return RealtimeContext.of("default", null);
        }
        return value;
    }

    private static Map<String, Object> immutableMetadata(Map<String, Object> value) {
        if (value == null) {
            return Map.of();
        }
        return Map.copyOf(value);
    }

    private static RealtimePayload copyPayload(RealtimePayload value) {
        if (value == null) {
            return RealtimePayload.text("");
        }
        return new RealtimePayload(value);
    }

    private static Instant defaultTimestamp(Instant value) {
        if (value == null) {
            return Instant.now();
        }
        return value;
    }
}
