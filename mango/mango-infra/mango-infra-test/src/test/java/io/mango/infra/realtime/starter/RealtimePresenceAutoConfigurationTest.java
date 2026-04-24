package io.mango.infra.realtime.starter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.infra.realtime.core.presence.IRealtimePresenceService;
import io.mango.infra.realtime.starter.presence.KvRealtimePresenceService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimePresenceAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MangoRealtimeAutoConfiguration.class))
            .withPropertyValues(
                    "mango.infra.realtime.mode=polling",
                    "mango.infra.realtime.polling.enabled=true");

    @Test
    void realtimePresenceService_usesKvWhenKvStoreSupportsSortedSet() {
        contextRunner
                .withBean(IKvStore.class, MemoryKvStore::new)
                .run(context -> assertThat(context.getBean(IRealtimePresenceService.class))
                        .isInstanceOf(KvRealtimePresenceService.class));
    }

    @Test
    void realtimePresenceService_fallsBackToMemoryWithoutKvStore() {
        contextRunner.run(context -> assertThat(context.getBean(IRealtimePresenceService.class))
                .isNotInstanceOf(KvRealtimePresenceService.class));
    }
}
