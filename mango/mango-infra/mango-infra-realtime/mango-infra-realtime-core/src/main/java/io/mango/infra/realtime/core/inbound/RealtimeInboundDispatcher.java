package io.mango.infra.realtime.core.inbound;

import io.mango.infra.realtime.api.RealtimeInboundMessage;

public interface RealtimeInboundDispatcher {

    void dispatch(RealtimeInboundMessage message);

    boolean hasListeners();
}
