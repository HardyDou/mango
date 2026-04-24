package io.mango.infra.realtime.api;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;

/**
 * Cross-module inbound contract used by local and remote realtime adapters.
 */
public interface RealtimeInboundApi {
    void dispatch(RealtimeInboundMessage message);
}
