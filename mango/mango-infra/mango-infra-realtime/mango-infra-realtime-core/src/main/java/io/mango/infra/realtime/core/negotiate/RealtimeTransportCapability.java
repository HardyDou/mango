package io.mango.infra.realtime.core.negotiate;

/**
 * One realtime transport capability exposed by the negotiation endpoint.
 */
public record RealtimeTransportCapability(
        String type,
        boolean enabled,
        String endpoint,
        boolean bidirectional,
        boolean longPolling,
        Integer defaultMaxSize,
        Integer maxSize,
        Long defaultTimeoutMillis,
        Long maxTimeoutMillis,
        boolean available,
        String reason,
        boolean serverEnabled,
        boolean clientSupported,
        boolean contextReady,
        boolean handshakeRequired,
        boolean probeRequired,
        String probeEndpoint) {

    public RealtimeTransportCapability {
        if (reason == null) {
            reason = "";
        }
        if (probeEndpoint == null) {
            probeEndpoint = "";
        }
    }

    public RealtimeTransportCapability(
            String type,
            boolean enabled,
            String endpoint,
            boolean bidirectional,
            boolean longPolling,
            Integer defaultMaxSize,
            Integer maxSize,
            Long defaultTimeoutMillis,
            Long maxTimeoutMillis) {
        this(type, enabled, endpoint, bidirectional, longPolling, defaultMaxSize, maxSize,
                defaultTimeoutMillis, maxTimeoutMillis, enabled, "", enabled, true, true,
                false, false, "");
    }
}
