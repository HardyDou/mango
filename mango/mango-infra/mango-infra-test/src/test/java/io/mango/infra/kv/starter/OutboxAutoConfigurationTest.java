package io.mango.infra.kv.starter;

import io.mango.infra.kv.api.IOutboxPublisher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxMessageQuery;
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

    @Test
    void outboxStore_shouldClaimByEventTypeWithoutLockingOtherMessages() {
        runner.withPropertyValues(
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.outbox=true")
                .run(context -> {
                    IOutboxStore store = context.getBean(IOutboxStore.class);
                    OutboxMessage realtimeMessage = OutboxMessage.builder()
                            .eventType("realtime.message.dispatch")
                            .businessType("realtime")
                            .businessKey("RT-1")
                            .aggregateId("USER:1001")
                            .occurredAt(Instant.parse("2026-05-16T02:00:00Z"))
                            .build();
                    OutboxMessage workflowMessage = OutboxMessage.builder()
                            .eventType("workflow.process.completed")
                            .businessType("workflow")
                            .businessKey("EXP-3")
                            .aggregateId("PROC-3")
                            .occurredAt(Instant.parse("2026-05-16T02:00:01Z"))
                            .build();

                    store.enqueue(realtimeMessage);
                    store.enqueue(workflowMessage);

                    var realtimeClaimed = store.claim(
                            "realtime-worker",
                            "realtime.message.dispatch",
                            10,
                            Instant.parse("2026-05-16T02:01:00Z"));
                    assertThat(realtimeClaimed)
                            .hasSize(1)
                            .first()
                            .extracting(OutboxMessage::getMessageId)
                            .isEqualTo(realtimeMessage.getMessageId());

                    store.ack(realtimeMessage.getMessageId(), "realtime-worker", Instant.parse("2026-05-16T02:02:00Z"));

                    var remaining = store.claim("workflow-worker", 10, Instant.parse("2026-05-16T02:03:00Z"));
                    assertThat(remaining)
                            .hasSize(1)
                            .first()
                            .extracting(OutboxMessage::getMessageId)
                            .isEqualTo(workflowMessage.getMessageId());
                });
    }

    @Test
    void outboxStore_shouldSupportFinalFailQueryAndRequeue() {
        runner.withPropertyValues(
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.outbox=true")
                .run(context -> {
                    IOutboxStore store = context.getBean(IOutboxStore.class);
                    OutboxMessage message = OutboxMessage.builder()
                            .eventType("workflow.process.completed")
                            .businessType("workflow")
                            .businessKey("EXP-4")
                            .aggregateId("PROC-4")
                            .occurredAt(Instant.parse("2026-05-16T03:00:00Z"))
                            .build();

                    store.enqueue(message);
                    var claimed = store.claim("worker-a", 10, Instant.parse("2026-05-16T03:01:00Z"));
                    assertThat(claimed).hasSize(1);

                    store.fail(
                            message.getMessageId(),
                            "worker-a",
                            "subscriber failed",
                            Instant.parse("2026-05-16T03:02:00Z"));

                    OutboxMessage failed = store.findById(message.getMessageId());
                    assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);
                    assertThat(failed.getErrorMessage()).isEqualTo("subscriber failed");
                    assertThat(store.claim("worker-b", 10, Instant.parse("2026-05-16T03:03:00Z"))).isEmpty();

                    OutboxMessageQuery query = new OutboxMessageQuery();
                    query.setStatus(OutboxStatus.FAILED);
                    query.setAbnormalOnly(true);
                    query.setKeyword("EXP-4");
                    assertThat(store.count(query)).isEqualTo(1L);
                    assertThat(store.query(query))
                            .hasSize(1)
                            .first()
                            .extracting(OutboxMessage::getMessageId)
                            .isEqualTo(message.getMessageId());

                    store.requeue(
                            message.getMessageId(),
                            Instant.parse("2026-05-16T03:05:00Z"),
                            Instant.parse("2026-05-16T03:04:00Z"));

                    OutboxMessage requeued = store.findById(message.getMessageId());
                    assertThat(requeued.getStatus()).isEqualTo(OutboxStatus.PENDING);
                    assertThat(requeued.getErrorMessage()).isNull();
                    assertThat(store.claim("worker-c", 10, Instant.parse("2026-05-16T03:04:59Z"))).isEmpty();
                    assertThat(store.claim("worker-c", 10, Instant.parse("2026-05-16T03:05:00Z"))).hasSize(1);
                });
    }
}
