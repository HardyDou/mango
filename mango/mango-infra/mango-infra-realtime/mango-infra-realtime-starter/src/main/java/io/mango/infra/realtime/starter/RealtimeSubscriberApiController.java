package io.mango.infra.realtime.starter;

import io.mango.infra.realtime.api.RealtimeSubscriberApi;
import io.mango.infra.realtime.api.RealtimeSubscriberRegistration;
import io.mango.infra.realtime.core.inbound.RealtimeSubscriberRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal subscriber-service registry endpoint for remote starter calls.
 */
@RestController
@RequestMapping("/internal/realtime/subscribers")
@RequiredArgsConstructor
public class RealtimeSubscriberApiController implements RealtimeSubscriberApi {

    private final RealtimeSubscriberRegistry subscriberRegistry;

    @Override
    @PostMapping("/register")
    public void register(@RequestBody RealtimeSubscriberRegistration registration) {
        subscriberRegistry.register(registration);
    }

    @Override
    @PostMapping("/unregister")
    public void unregister(@RequestBody RealtimeSubscriberRegistration registration) {
        subscriberRegistry.unregister(registration);
    }
}
