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
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
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

    private static String ensureLeadingSlash(String value) {
        if (value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }
}
