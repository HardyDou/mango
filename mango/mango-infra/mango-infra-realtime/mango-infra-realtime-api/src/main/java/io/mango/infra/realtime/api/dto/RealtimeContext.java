package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "实时消息上下文")
public record RealtimeContext(
        @Schema(description = "租户ID")
        String tenantId,
        @Schema(description = "用户ID")
        Long userId,
        @Schema(description = "链路追踪ID")
        String traceId,
        @Schema(description = "请求ID")
        String requestId) {

    public RealtimeContext {
        tenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }

    public static RealtimeContext of(String tenantId, Long userId) {
        return new RealtimeContext(tenantId, userId, null, null);
    }
}
