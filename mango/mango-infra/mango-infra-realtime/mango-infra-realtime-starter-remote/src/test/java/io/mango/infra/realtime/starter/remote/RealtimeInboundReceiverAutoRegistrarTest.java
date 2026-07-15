package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;
import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
class RealtimeInboundReceiverAutoRegistrarTest {

    @Test
    void shutdownContinuesWhenRemoteRegistryIsUnavailable() {
        RealtimeInboundReceiverFeignClient client = unavailableRegistry();
        IRealtimeInboundService inboundService = activeInboundService();
        RealtimeRemoteProperties properties = new RealtimeRemoteProperties();
        properties.getInbound().setEnabled(true);
        RealtimeInboundReceiverAutoRegistrar registrar = new RealtimeInboundReceiverAutoRegistrar(
                client, inboundService, properties, new MockEnvironment().withProperty("spring.application.name", "demo"));

        assertThatCode(registrar::unregister).doesNotThrowAnyException();
    }

    private RealtimeInboundReceiverFeignClient unavailableRegistry() {
        return new RealtimeInboundReceiverFeignClient() {
            @Override
            public void register(RealtimeInboundReceiverRegistration registration) {
                // Not used by this shutdown test.
            }

            @Override
            public void unregister(RealtimeInboundReceiverRegistration registration) {
                throw new IllegalStateException("registry unavailable");
            }
        };
    }

    private IRealtimeInboundService activeInboundService() {
        return new IRealtimeInboundService() {
            @Override
            public void dispatch(RealtimeInboundMessage message) {
                // Not used by this shutdown test.
            }

            @Override
            public boolean hasListeners() {
                return true;
            }
        };
    }
}
