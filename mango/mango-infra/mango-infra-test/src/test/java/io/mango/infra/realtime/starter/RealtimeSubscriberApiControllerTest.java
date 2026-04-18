package io.mango.infra.realtime.starter;

import io.mango.infra.realtime.api.RealtimeSubscriberRegistration;
import io.mango.infra.realtime.core.inbound.InMemoryRealtimeSubscriberRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealtimeSubscriberApiControllerTest {

    @Test
    void registerAndUnregister_delegatesToRegistry() {
        InMemoryRealtimeSubscriberRegistry registry = new InMemoryRealtimeSubscriberRegistry();
        RealtimeSubscriberApiController controller = new RealtimeSubscriberApiController(registry);
        RealtimeSubscriberRegistration registration =
                new RealtimeSubscriberRegistration("task-service", "/", "/internal/realtime/inbound");

        controller.register(registration);
        assertEquals(1, registry.findAll().size());

        controller.unregister(registration);
        assertEquals(0, registry.findAll().size());
    }
}
