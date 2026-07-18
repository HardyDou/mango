package io.mango.infra.realtime.support.inbound;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;

@LocalCapabilityContract
public interface IRealtimeInboundService {

    void dispatch(RealtimeInboundMessage message);

    boolean hasListeners();
}
