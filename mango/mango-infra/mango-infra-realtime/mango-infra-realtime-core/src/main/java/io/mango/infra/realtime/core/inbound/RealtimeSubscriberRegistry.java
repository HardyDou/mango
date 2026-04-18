package io.mango.infra.realtime.core.inbound;

import io.mango.infra.realtime.api.RealtimeSubscriberRegistration;

import java.util.Collection;

public interface RealtimeSubscriberRegistry {

    void register(RealtimeSubscriberRegistration registration);

    void unregister(RealtimeSubscriberRegistration registration);

    Collection<RealtimeSubscriberRegistration> findAll();
}
