package io.mango.infra.kv.starter;

import io.mango.infra.kv.api.IOutboxPublisher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KvStoreAutoConfiguration.class, OutboxAutoConfiguration.class));

    @Test
    void whenEnabled_shouldCreateOutboxStore() {
        runner.withPropertyValues(
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.outbox=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(IOutboxStore.class);
                    assertThat(context).hasSingleBean(IOutboxPublisher.class);
                });
    }

    @Test
    void outboxStore_shouldSupportBasicLifecycle() {
        runner.withPropertyValues(
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.outbox=true")
                .run(context -> {
                    IOutboxStore store = context.getBean(IOutboxStore.class);
                    OutboxMessage message = OutboxMessage.builder()
                            .eventType("workflow.process.completed")
                            .businessType("workflow")
                            .businessKey("EXP-1")
                            .aggregateId("PROC-1")
                            .occurredAt(Instant.parse("2026-05-16T00:00:00Z"))
                            .build();

                    store.enqueue(message);

                    var claimed = store.claim("worker-a", 10, Instant.parse("2026-05-16T00:01:00Z"));
                    assertThat(claimed).hasSize(1);
                    assertThat(claimed.get(0).getStatus()).isEqualTo(OutboxStatus.PROCESSING);

                    store.ack(message.getMessageId(), "worker-a", Instant.parse("2026-05-16T00:02:00Z"));
                    var afterAck = store.claim("worker-a", 10, Instant.parse("2026-05-16T00:03:00Z"));
                    assertThat(afterAck).isEmpty();
                });
    }

    @Test
    void outboxStore_shouldSupportNackAndRetry() {
        runner.withPropertyValues(
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.outbox=true")
                .run(context -> {
                    IOutboxStore store = context.getBean(IOutboxStore.class);
                    OutboxMessage message = OutboxMessage.builder()
                            .eventType("workflow.process.rejected")
                            .businessType("workflow")
                            .businessKey("EXP-2")
                            .aggregateId("PROC-2")
                            .occurredAt(Instant.parse("2026-05-16T01:00:00Z"))
                            .build();

                    store.enqueue(message);

                    var claimed = store.claim("worker-a", 10, Instant.parse("2026-05-16T01:01:00Z"));
                    assertThat(claimed).hasSize(1);

                    store.nack(
                            message.getMessageId(),
                            "worker-a",
                            "temporary failure",
                            Instant.parse("2026-05-16T01:05:00Z"),
                            Instant.parse("2026-05-16T01:02:00Z"));

                    assertThat(store.claim("worker-b", 10, Instant.parse("2026-05-16T01:04:59Z"))).isEmpty();

                    var retried = store.claim("worker-b", 10, Instant.parse("2026-05-16T01:05:00Z"));
                    assertThat(retried).hasSize(1);
                    assertThat(retried.get(0).getStatus()).isEqualTo(OutboxStatus.PROCESSING);
                    assertThat(retried.get(0).getAttemptCount()).isEqualTo(2);
                    assertThat(retried.get(0).getErrorMessage()).isEqualTo("temporary failure");
                });
    }
}
