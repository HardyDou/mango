package io.mango.infra.realtime.core.inbound.receiver;

import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;

import java.util.Collection;

public interface IRealtimeInboundReceiverService {

    void register(RealtimeInboundReceiverRegistration registration);

    void unregister(RealtimeInboundReceiverRegistration registration);

    Collection<RealtimeInboundReceiverRegistration> findAll();
}
