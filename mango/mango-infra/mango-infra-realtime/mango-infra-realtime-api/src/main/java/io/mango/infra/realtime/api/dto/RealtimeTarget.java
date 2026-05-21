package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "实时消息投递目标")
public record RealtimeTarget(
        @Schema(description = "目标类型")
        RealtimeTargetType type,
        @Schema(description = "目标标识")
        String id) {

    public RealtimeTarget {
        type = type == null ? RealtimeTargetType.BROADCAST : type;
        id = id == null ? "" : id.trim();
    }

    public static RealtimeTarget user(Long userId) {
        return new RealtimeTarget(RealtimeTargetType.USER, userId == null ? "" : String.valueOf(userId));
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
}
