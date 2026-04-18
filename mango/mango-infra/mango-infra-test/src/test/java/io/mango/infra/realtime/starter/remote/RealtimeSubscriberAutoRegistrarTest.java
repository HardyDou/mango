package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.RealtimeSubscriberRegistration;
import io.mango.infra.realtime.core.inbound.RealtimeInboundDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeSubscriberAutoRegistrarTest {

    @Test
    void register_hasListeners_registersService() {
        RealtimeSubscriberFeignClient subscriberApi = mock(RealtimeSubscriberFeignClient.class);
        RealtimeInboundDispatcher dispatcher = mock(RealtimeInboundDispatcher.class);
        when(dispatcher.hasListeners()).thenReturn(true);
        RealtimeRemoteProperties properties = new RealtimeRemoteProperties();
        properties.getInbound().setEnabled(true);
        MockEnvironment environment = new MockEnvironment().withProperty("spring.application.name", "task-service");
        RealtimeSubscriberAutoRegistrar registrar =
                new RealtimeSubscriberAutoRegistrar(subscriberApi, dispatcher, properties, environment);

        registrar.register();

        verify(subscriberApi).register(argThat(this::isTaskServiceRegistration));
    }

    @Test
    void register_noListeners_doesNotRegister() {
        RealtimeSubscriberFeignClient subscriberApi = mock(RealtimeSubscriberFeignClient.class);
        RealtimeInboundDispatcher dispatcher = mock(RealtimeInboundDispatcher.class);
        when(dispatcher.hasListeners()).thenReturn(false);
        RealtimeRemoteProperties properties = new RealtimeRemoteProperties();
        properties.getInbound().setEnabled(true);
        RealtimeSubscriberAutoRegistrar registrar =
                new RealtimeSubscriberAutoRegistrar(subscriberApi, dispatcher, properties, new MockEnvironment());

        registrar.register();

        verify(subscriberApi, never()).register(org.mockito.Mockito.any());
    }

    private boolean isTaskServiceRegistration(RealtimeSubscriberRegistration registration) {
        return "task-service".equals(registration.serviceName())
                && "/".equals(registration.contextPath())
                && "/internal/realtime/inbound".equals(registration.endpoint());
    }
}
