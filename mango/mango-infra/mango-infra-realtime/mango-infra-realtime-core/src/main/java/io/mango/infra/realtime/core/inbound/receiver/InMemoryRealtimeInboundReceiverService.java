package io.mango.infra.realtime.core.inbound.receiver;

import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRealtimeInboundReceiverService implements IRealtimeInboundReceiverService {

    private final ConcurrentHashMap<String, RealtimeInboundReceiverRegistration> registrations = new ConcurrentHashMap<>();

    @Override
    public void register(RealtimeInboundReceiverRegistration registration) {
        if (registration == null || registration.serviceName() == null || registration.serviceName().isBlank()) {
            return;
        }
        registrations.put(registrationKey(registration), registration);
    }

    @Override
    public void unregister(RealtimeInboundReceiverRegistration registration) {
        if (registration == null) {
            return;
        }
        registrations.remove(registrationKey(registration));
    }

    @Override
    public Collection<RealtimeInboundReceiverRegistration> findAll() {
        return registrations.values();
    }

    private String registrationKey(RealtimeInboundReceiverRegistration registration) {
        return registration.serviceName() + "|" + registration.contextPath() + "|" + registration.endpoint();
    }
}
