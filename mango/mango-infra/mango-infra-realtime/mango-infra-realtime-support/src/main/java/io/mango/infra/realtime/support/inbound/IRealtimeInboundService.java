package io.mango.infra.realtime.support.inbound;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;

public interface IRealtimeInboundService {

    void dispatch(RealtimeInboundMessage message);

    boolean hasListeners();
}
