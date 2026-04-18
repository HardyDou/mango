package io.mango.infra.realtime.core.negotiate;

import java.util.List;

/**
 * HTTP response body returned by the realtime transport negotiation endpoint.
 */
public record RealtimeNegotiationResponse(
        String recommended,
        List<RealtimeTransportCapability> transports) {
}
