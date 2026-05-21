package io.mango.infra.realtime.core.negotiate;

import java.util.List;

/**
 * HTTP response body returned by the realtime transport negotiation endpoint.
 */
public record RealtimeNegotiationResponse(
        String recommended,
        List<RealtimeTransportCapability> transports,
        List<String> order,
        String connectionTicket,
        Long ticketExpiresAt) {

    public RealtimeNegotiationResponse(String recommended, List<RealtimeTransportCapability> transports) {
        this(recommended, transports, List.of(), null, null);
    }
}
