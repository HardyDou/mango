package io.mango.infra.realtime.starter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.infra.realtime.core.inbound.forward.ProtocolRealtimeInboundForwarder;
import io.mango.infra.realtime.core.polling.InMemoryRealtimePollingService;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;
import io.mango.infra.realtime.starter.controller.PollingRealtimeController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MangoRealtimePollingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MangoRealtimeAutoConfiguration.class))
            .withPropertyValues(
                    "mango.infra.realtime.mode=polling",
                    "mango.infra.realtime.polling.enabled=true")
            .withBean(IKvStore.class, MemoryKvStore::new);

    @Test
    void startsPollingOnlyModeWithoutConnectionSubscriptionManager() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(InMemoryRealtimePollingService.class);
            assertThat(context).hasSingleBean(ProtocolRealtimeInboundForwarder.class);
            assertThat(context).hasSingleBean(PollingRealtimeController.class);
            assertThat(context).doesNotHaveBean(RealtimeSubscriptionManager.class);
        });
    }
}
