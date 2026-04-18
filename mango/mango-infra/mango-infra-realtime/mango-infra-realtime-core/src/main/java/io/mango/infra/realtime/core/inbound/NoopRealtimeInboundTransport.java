package io.mango.infra.realtime.core.inbound;

import io.mango.infra.realtime.api.RealtimeInboundMessage;

public class NoopRealtimeInboundTransport implements RealtimeInboundTransport {

    @Override
    public void forward(RealtimeInboundMessage message) {
        // Inbound dispatch is disabled.
    }
}
