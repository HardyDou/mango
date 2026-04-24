package io.mango.infra.realtime.api;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

/**
 * Reverse outbound contract used by realtime instances to deliver messages to the node holding local sessions.
 */
public interface RealtimeOutboundApi {

    void dispatch(RealtimeOutboundMessage message);
}
