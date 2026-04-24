package io.mango.infra.realtime.api.dto;

/**
 * One service that can receive client-to-server realtime messages.
 */
public record RealtimeInboundReceiverRegistration(
        String serviceName,
        String contextPath,
        String endpoint) {

    public RealtimeInboundReceiverRegistration {
        contextPath = contextPath == null || contextPath.isBlank() ? "/" : contextPath;
        endpoint = endpoint == null || endpoint.isBlank() ? "/_realtime/messages/inbound" : endpoint;
    }
}
