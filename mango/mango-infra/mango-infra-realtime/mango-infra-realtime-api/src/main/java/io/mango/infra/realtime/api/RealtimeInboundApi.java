package io.mango.infra.realtime.api;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;

/**
 * Cross-module inbound contract used by local and remote realtime adapters.
 */
@LocalCapabilityContract
public interface RealtimeInboundApi {
    void dispatch(RealtimeInboundMessage message);
}
