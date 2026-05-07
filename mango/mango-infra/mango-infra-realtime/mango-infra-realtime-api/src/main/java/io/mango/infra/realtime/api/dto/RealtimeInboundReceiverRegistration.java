package io.mango.infra.realtime.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One service that can receive client-to-server realtime messages.
 */
@Schema(description = "实时入站接收器注册信息")
public record RealtimeInboundReceiverRegistration(
        @Schema(description = "服务名称")
        String serviceName,
        @Schema(description = "服务上下文路径")
        String contextPath,
        @Schema(description = "入站接收端点")
        String endpoint) {

    public RealtimeInboundReceiverRegistration {
        contextPath = contextPath == null || contextPath.isBlank() ? "/" : contextPath;
        endpoint = endpoint == null || endpoint.isBlank() ? "/_realtime/messages/inbound" : endpoint;
    }
}
