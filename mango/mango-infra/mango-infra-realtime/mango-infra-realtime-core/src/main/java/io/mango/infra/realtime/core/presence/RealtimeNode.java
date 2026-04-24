package io.mango.infra.realtime.core.presence;

/**
 * Runtime identity of a realtime service instance.
 */
public record RealtimeNode(
        String instanceId,
        String serviceName,
        String contextPath,
        String outboundEndpoint) {

    public RealtimeNode {
        instanceId = blankToDefault(instanceId, "local");
        serviceName = blankToDefault(serviceName, "application");
        contextPath = normalizeContextPath(contextPath);
        outboundEndpoint = normalizeEndpoint(outboundEndpoint);
    }

    public boolean isLocal(RealtimePresence presence) {
        return presence != null && instanceId.equals(presence.instanceId());
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String normalizeContextPath(String value) {
        if (value == null || value.isBlank() || "/".equals(value)) {
            return "";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String normalizeEndpoint(String value) {
        if (value == null || value.isBlank()) {
            return "/_realtime/messages/outbound";
        }
        return value.startsWith("/") ? value : "/" + value;
    }
}
