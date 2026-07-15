package io.mango.infra.realtime.core.presence;

import java.time.Instant;

/**
 * Serializable route metadata for one online realtime session.
 */
public record RealtimePresence(
        String sessionId,
        String tenantId,
        Long userId,
        String clientId,
        String protocol,
        String instanceId,
        String serviceName,
        String contextPath,
        String outboundEndpoint,
        Instant lastSeenAt) {

    public RealtimePresence {
        tenantId = defaultTenantId(tenantId);
        contextPath = normalizeContextPath(contextPath);
        outboundEndpoint = normalizeEndpoint(outboundEndpoint);
        lastSeenAt = defaultLastSeenAt(lastSeenAt);
    }

    public static RealtimePresence of(String sessionId,
                                      String tenantId,
                                      Long userId,
                                      String clientId,
                                      String protocol,
                                      RealtimeNode node) {
        return new RealtimePresence(
                sessionId,
                tenantId,
                userId,
                clientId,
                protocol,
                node.instanceId(),
                node.serviceName(),
                node.contextPath(),
                node.outboundEndpoint(),
                null);
    }

    public String routeKey() {
        return serviceName + "|" + contextPath + "|" + outboundEndpoint + "|" + instanceId;
    }

    private static String normalizeContextPath(String value) {
        if (value == null || value.isBlank() || "/".equals(value)) {
            return "";
        }
        return ensureLeadingSlash(value);
    }

    private static String normalizeEndpoint(String value) {
        if (value == null || value.isBlank()) {
            return "/_realtime/messages/outbound";
        }
        return ensureLeadingSlash(value);
    }

    private static String defaultTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "default";
        }
        return tenantId;
    }

    private static Instant defaultLastSeenAt(Instant candidate) {
        if (candidate == null) {
            return Instant.now();
        }
        return candidate;
    }

    private static String ensureLeadingSlash(String value) {
        if (value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }
}
