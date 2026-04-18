package io.mango.infra.realtime.core.inbound;

import io.mango.infra.realtime.api.RealtimeInboundMessage;

public interface RealtimeInboundTransport {

    void forward(RealtimeInboundMessage message);
}
