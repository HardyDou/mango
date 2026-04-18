package io.mango.infra.realtime.api;

/**
 * One service that can receive client-to-server realtime messages.
 */
public record RealtimeSubscriberRegistration(
        String serviceName,
        String contextPath,
        String endpoint) {

    public RealtimeSubscriberRegistration {
        contextPath = contextPath == null || contextPath.isBlank() ? "/" : contextPath;
        endpoint = endpoint == null || endpoint.isBlank() ? "/internal/realtime/inbound" : endpoint;
    }
}
