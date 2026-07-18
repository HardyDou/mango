package io.mango.infra.realtime.api;

import io.mango.common.contract.LocalCapabilityContract;
import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;

/**
 * Cross-module API used to register and unregister inbound receiver services.
 */
@LocalCapabilityContract
public interface RealtimeInboundReceiverApi {

    void register(RealtimeInboundReceiverRegistration registration);

    void unregister(RealtimeInboundReceiverRegistration registration);
}
