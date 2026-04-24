package io.mango.infra.realtime.api;

import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;

/**
 * Cross-module API used to register and unregister inbound receiver services.
 */
public interface RealtimeInboundReceiverApi {

    void register(RealtimeInboundReceiverRegistration registration);

    void unregister(RealtimeInboundReceiverRegistration registration);
}
