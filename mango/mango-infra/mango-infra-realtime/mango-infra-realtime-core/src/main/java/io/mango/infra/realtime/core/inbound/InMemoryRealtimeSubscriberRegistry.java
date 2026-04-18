package io.mango.infra.realtime.core.inbound;

import io.mango.infra.realtime.api.RealtimeSubscriberRegistration;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRealtimeSubscriberRegistry implements RealtimeSubscriberRegistry {

    private final ConcurrentHashMap<String, RealtimeSubscriberRegistration> registrations = new ConcurrentHashMap<>();

    @Override
    public void register(RealtimeSubscriberRegistration registration) {
        if (registration == null || registration.serviceName() == null || registration.serviceName().isBlank()) {
            return;
        }
        registrations.put(registrationKey(registration), registration);
    }

    @Override
    public void unregister(RealtimeSubscriberRegistration registration) {
        if (registration == null) {
            return;
        }
        registrations.remove(registrationKey(registration));
    }

    @Override
    public Collection<RealtimeSubscriberRegistration> findAll() {
        return registrations.values();
    }

    private String registrationKey(RealtimeSubscriberRegistration registration) {
        return registration.serviceName() + "|" + registration.contextPath() + "|" + registration.endpoint();
    }
}
