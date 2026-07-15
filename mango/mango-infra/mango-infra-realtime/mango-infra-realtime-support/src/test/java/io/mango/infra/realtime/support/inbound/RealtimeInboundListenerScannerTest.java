package io.mango.infra.realtime.support.inbound;

import io.mango.infra.realtime.api.annotation.RealtimeInboundMessageListener;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeInboundListenerScannerTest {

    @Test
    void scansListenerWithoutInstantiatingUnrelatedLazyBeans() {
        AtomicInteger unrelatedCreations = new AtomicInteger();
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean("listener", Listener.class, Listener::new);
            context.registerBean("unrelated", Object.class, () -> {
                unrelatedCreations.incrementAndGet();
                return new Object();
            }, definition -> definition.setLazyInit(true));
            context.refresh();

            var listeners = new RealtimeInboundListenerScanner().scan(context);

            assertThat(listeners).containsKey("task.cancel");
            assertThat(unrelatedCreations).hasValue(0);
        }
    }

    static final class Listener {
        @RealtimeInboundMessageListener(types = "task.cancel")
        void onMessage(RealtimeInboundMessage message) {
        }
    }
}
