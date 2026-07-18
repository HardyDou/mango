package io.mango.infra.realtime.api;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

/**
 * Reverse outbound contract used by realtime instances to deliver messages to the node holding local sessions.
 */
@LocalCapabilityContract
public interface RealtimeOutboundApi {

    void dispatch(RealtimeOutboundMessage message);
}
