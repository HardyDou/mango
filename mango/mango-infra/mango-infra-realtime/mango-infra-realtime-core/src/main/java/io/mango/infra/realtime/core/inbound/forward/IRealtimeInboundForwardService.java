package io.mango.infra.realtime.core.inbound.forward;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;

public interface IRealtimeInboundForwardService {

    void forward(RealtimeInboundMessage message);
}
