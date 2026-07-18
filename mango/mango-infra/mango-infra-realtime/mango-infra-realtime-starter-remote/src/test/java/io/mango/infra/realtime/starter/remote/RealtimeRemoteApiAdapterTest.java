package io.mango.infra.realtime.starter.remote;

import static org.assertj.core.api.Assertions.assertThat;

import io.mango.infra.realtime.api.RealtimeInboundApi;
import io.mango.infra.realtime.api.RealtimeInboundReceiverApi;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeInboundReceiverRegistration;
import io.mango.infra.realtime.support.inbound.IRealtimeInboundService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RealtimeRemoteApiAdapterTest {

    private final RealtimeRemoteAutoConfiguration configuration =
            new RealtimeRemoteAutoConfiguration();

    @Test
    void inboundApiDelegatesToLocalListenerService() {
        AtomicInteger dispatchCount = new AtomicInteger();
        IRealtimeInboundService inboundService = new IRealtimeInboundService() {
            @Override
            public void dispatch(RealtimeInboundMessage message) {
                dispatchCount.incrementAndGet();
            }

            @Override
            public boolean hasListeners() {
                return true;
            }
        };

        RealtimeInboundApi api = configuration.realtimeInboundApi(inboundService);
        api.dispatch(null);

        assertThat(dispatchCount).hasValue(1);
    }

    @Test
    void receiverApiDelegatesBothFeignOperations() {
        AtomicInteger registerCount = new AtomicInteger();
        AtomicInteger unregisterCount = new AtomicInteger();
        RealtimeInboundReceiverFeignClient client =
                new RealtimeInboundReceiverFeignClient() {
                    @Override
                    public void register(RealtimeInboundReceiverRegistration registration) {
                        registerCount.incrementAndGet();
                    }

                    @Override
                    public void unregister(RealtimeInboundReceiverRegistration registration) {
                        unregisterCount.incrementAndGet();
                    }
                };

        RealtimeInboundReceiverApi api = configuration.realtimeInboundReceiverApi(client);
        api.register(null);
        api.unregister(null);

        assertThat(registerCount).hasValue(1);
        assertThat(unregisterCount).hasValue(1);
    }
}
