package io.mango.infra.realtime.starter;

import static org.assertj.core.api.Assertions.assertThat;

import io.mango.infra.realtime.api.RealtimeInboundReceiverApi;
import io.mango.infra.realtime.api.RealtimeOutboundApi;
import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.inbound.receiver.IRealtimeInboundReceiverService;
import io.mango.infra.realtime.core.outbound.IRealtimePublishService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RealtimeLocalApiAdapterTest {

    private final MangoRealtimeAutoConfiguration configuration =
            new MangoRealtimeAutoConfiguration();

    @Test
    void outboundApiDelegatesToLocalPublishing() {
        AtomicInteger localPublishCount = new AtomicInteger();
        IRealtimePublishService publishService = new IRealtimePublishService() {
            @Override
            public void publish(RealtimeOutboundMessage message) {
            }

            @Override
            public void publishLocal(RealtimeOutboundMessage message) {
                localPublishCount.incrementAndGet();
            }
        };

        RealtimeOutboundApi api = configuration.realtimeOutboundApi(publishService);
        api.dispatch(null);

        assertThat(localPublishCount).hasValue(1);
    }

    @Test
    void receiverApiDelegatesBothRegistryOperations() {
        AtomicInteger registerCount = new AtomicInteger();
        AtomicInteger unregisterCount = new AtomicInteger();
        IRealtimeInboundReceiverService receiverService =
                new IRealtimeInboundReceiverService() {
                    @Override
                    public void register(RealtimeInboundReceiverRegistration registration) {
                        registerCount.incrementAndGet();
                    }

                    @Override
                    public void unregister(RealtimeInboundReceiverRegistration registration) {
                        unregisterCount.incrementAndGet();
                    }

                    @Override
                    public List<RealtimeInboundReceiverRegistration> findAll() {
                        return List.of();
                    }
                };

        RealtimeInboundReceiverApi api =
                configuration.realtimeInboundReceiverApi(receiverService);
        api.register(null);
        api.unregister(null);

        assertThat(registerCount).hasValue(1);
        assertThat(unregisterCount).hasValue(1);
    }
}
