package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "实时消息事件定义")
public record RealtimeEvent(
        @Schema(description = "事件域，如 chat/system/workflow/notification/agent")
        String domain,
        @Schema(description = "事件名，如 message.send/message.accepted")
        String name) {

    public RealtimeEvent {
        domain = domain == null || domain.isBlank() ? "default" : domain;
        name = name == null || name.isBlank() ? "message" : name;
    }

    public static RealtimeEvent of(String domain, String name) {
        return new RealtimeEvent(domain, name);
    }

    public static RealtimeEvent fromLegacyType(String type) {
        if (type == null || type.isBlank()) {
            return of("default", "message");
        }
        if ("connected".equals(type)) {
            return of("system", "connection.connected");
        }
        if ("pong".equals(type)) {
            return of("system", "heartbeat.pong");
        }
        if ("ping".equals(type)) {
            return of("system", "heartbeat.ping");
        }
        if ("error".equals(type)) {
            return of("system", "message.error");
        }
        if ("accepted".equals(type) || "ack".equals(type)) {
            return of("system", "message.accepted");
        }
        int separator = type.indexOf('.');
        if (separator > 0 && separator < type.length() - 1) {
            return of(type.substring(0, separator), type.substring(separator + 1));
        }
        return of("default", type);
    }
}
