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
        Long maxTimeoutMillis) {
}
