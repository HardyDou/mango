package io.mango.infra.event.starter;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventBus;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.event.core.outbox.OutboxDomainEventPublisher;
import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.starter.KvStoreAutoConfiguration;
import io.mango.infra.kv.starter.OutboxAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventOutboxAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KvStoreAutoConfiguration.class,
                    OutboxAutoConfiguration.class,
                    DomainEventAutoConfiguration.class));

    @Test
    void whenOutboxDisabled_shouldPublishDirectlyToMemoryBus() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(IDomainEventBus.class);
            assertThat(context).hasSingleBean(IDomainEventPublisher.class);
            assertThat(context.getBean(IDomainEventPublisher.class)).isSameAs(context.getBean(IDomainEventBus.class));
            assertThat(context).doesNotHaveBean(IOutboxDispatcher.class);
        });
    }

    @Test
    void whenOutboxEnabled_shouldPublishToOutboxAndDispatchToMemoryBus() {
        runner.withPropertyValues(
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.outbox=true",
                        "mango.event.outbox.enabled=true",
                        "mango.event.outbox.worker-id=test-event-worker",
                        "mango.event.outbox.batch-size=10")
                .run(context -> {
                    assertThat(context).hasSingleBean(IDomainEventBus.class);
                    assertThat(context.getBean(IDomainEventPublisher.class)).isInstanceOf(OutboxDomainEventPublisher.class);
                    assertThat(context).hasSingleBean(IOutboxDispatcher.class);
                    assertThat(context).hasSingleBean(OutboxDispatchScheduler.class);

                    ArrayList<DomainEvent> handled = new ArrayList<>();
                    IDomainEventBus eventBus = context.getBean(IDomainEventBus.class);
                    eventBus.subscribe("workflow.process.completed", handled::add);

                    IDomainEventPublisher publisher = context.getBean(IDomainEventPublisher.class);
                    publisher.publish(DomainEvent.builder()
                            .eventType("workflow.process.completed")
                            .businessType("EXPENSE_REIMBURSEMENT")
                            .businessKey("EXP-20260516-001")
                            .aggregateId("PROC-1")
                            .payload("amount", 1200)
                            .header("tenantId", "1")
                            .build());

                    assertThat(handled).isEmpty();

                    int dispatched = context.getBean(IOutboxDispatcher.class).dispatchOnce();

                    assertThat(dispatched).isEqualTo(1);
                    assertThat(handled).hasSize(1);
                    assertThat(handled.get(0).getBusinessKey()).isEqualTo("EXP-20260516-001");
                    assertThat(handled.get(0).getPayload()).containsEntry("amount", 1200);
                    assertThat(handled.get(0).getHeaders()).containsEntry("tenantId", "1");
                });
    }

    @Test
    void whenOutboxDispatchDisabled_shouldNotCreateScheduler() {
        runner.withPropertyValues(
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.outbox=true",
                        "mango.event.outbox.enabled=true",
                        "mango.event.outbox.dispatch-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(IOutboxDispatcher.class);
                    assertThat(context).doesNotHaveBean(OutboxDispatchScheduler.class);
                });
    }
}
