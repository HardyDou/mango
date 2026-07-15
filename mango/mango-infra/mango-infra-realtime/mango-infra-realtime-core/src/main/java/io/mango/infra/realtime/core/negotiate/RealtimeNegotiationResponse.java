package io.mango.infra.realtime.core.negotiate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * HTTP response body returned by the realtime transport negotiation endpoint.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "List components are defensively copied by the compact constructor")
public record RealtimeNegotiationResponse(
        String recommended,
        List<RealtimeTransportCapability> transports,
        List<String> order,
        String connectionTicket,
        Long ticketExpiresAt) {

    public RealtimeNegotiationResponse {
        transports = immutableList(transports);
        order = immutableList(order);
    }

    public RealtimeNegotiationResponse(String recommended, List<RealtimeTransportCapability> transports) {
        this(recommended, transports, List.of(), null, null);
    }

    private static <T> List<T> immutableList(List<T> source) {
        if (source == null) {
            return List.of();
        }
        return List.copyOf(source);
    }
}
