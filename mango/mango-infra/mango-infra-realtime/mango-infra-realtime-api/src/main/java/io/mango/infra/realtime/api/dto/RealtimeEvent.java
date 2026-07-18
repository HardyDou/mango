package io.mango.infra.realtime.api.dto;

import io.mango.common.contract.LocalCapabilityContract;
import io.swagger.v3.oas.annotations.media.Schema;

@LocalCapabilityContract
@Schema(description = "实时消息事件定义")
public record RealtimeEvent(
        @Schema(description = "事件域，如 chat/system/workflow/notification/agent")
        String domain,
        @Schema(description = "事件名，如 message.send/message.accepted")
        String name) {

    public RealtimeEvent {
        domain = defaultIfBlank(domain, "default");
        name = defaultIfBlank(name, "message");
    }

    public static RealtimeEvent of(String domain, String name) {
        return new RealtimeEvent(domain, name);
    }

    public static RealtimeEvent fromLegacyType(String type) {
        if (type == null || type.isBlank()) {
            return of("default", "message");
        }
        return switch (type) {
            case "connected" -> of("system", "connection.connected");
            case "pong" -> of("system", "heartbeat.pong");
            case "ping" -> of("system", "heartbeat.ping");
            case "error" -> of("system", "message.error");
            case "accepted", "ack" -> of("system", "message.accepted");
            default -> fromQualifiedType(type);
        };
    }

    private static RealtimeEvent fromQualifiedType(String type) {
        int separator = type.indexOf('.');
        if (separator > 0 && separator < type.length() - 1) {
            return of(type.substring(0, separator), type.substring(separator + 1));
        }
        return of("default", type);
    }

    private static String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
