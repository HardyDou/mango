package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "实时消息投递目标")
public record RealtimeTarget(
        @Schema(description = "目标类型")
        RealtimeTargetType type,
        @Schema(description = "目标标识")
        String id) {

    public RealtimeTarget {
        if (type == null) {
            type = RealtimeTargetType.BROADCAST;
        }
        id = normalizedId(id);
    }

    public static RealtimeTarget user(Long userId) {
        return new RealtimeTarget(RealtimeTargetType.USER, stringValue(userId));
    }

    public static RealtimeTarget client(String clientId) {
        return new RealtimeTarget(RealtimeTargetType.CLIENT, clientId);
    }

    public static RealtimeTarget connection(String connectionId) {
        return new RealtimeTarget(RealtimeTargetType.CONNECTION, connectionId);
    }

    public static RealtimeTarget group(String groupId) {
        return new RealtimeTarget(RealtimeTargetType.GROUP, groupId);
    }

    public static RealtimeTarget tenant(String tenantId) {
        return new RealtimeTarget(RealtimeTargetType.TENANT, tenantId);
    }

    public static RealtimeTarget broadcast() {
        return new RealtimeTarget(RealtimeTargetType.BROADCAST, "");
    }

    private static String normalizedId(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String stringValue(Long value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }
}
