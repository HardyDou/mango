package io.mango.infra.realtime.core.inbound;

import io.mango.infra.realtime.api.RealtimeSubscriberRegistration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryRealtimeSubscriberRegistryTest {

    @Test
    void register_sameServiceEndpoint_isIdempotent() {
        InMemoryRealtimeSubscriberRegistry registry = new InMemoryRealtimeSubscriberRegistry();
        RealtimeSubscriberRegistration registration =
                new RealtimeSubscriberRegistration("task-service", "/", "/internal/realtime/inbound");

        registry.register(registration);
        registry.register(registration);

        assertEquals(1, registry.findAll().size());
    }

    @Test
    void unregister_existingRegistration_removesIt() {
        InMemoryRealtimeSubscriberRegistry registry = new InMemoryRealtimeSubscriberRegistry();
        RealtimeSubscriberRegistration registration =
                new RealtimeSubscriberRegistration("task-service", "/", "/internal/realtime/inbound");

        registry.register(registration);
        registry.unregister(registration);

        assertEquals(0, registry.findAll().size());
    }
}
