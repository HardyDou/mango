package io.mango.infra.realtime.core.inbound;

import io.mango.infra.realtime.api.RealtimeInboundMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalRealtimeInboundTransport implements RealtimeInboundTransport {

    private final RealtimeInboundDispatcher dispatcher;

    @Override
    public void forward(RealtimeInboundMessage message) {
        dispatcher.dispatch(message);
    }
}
