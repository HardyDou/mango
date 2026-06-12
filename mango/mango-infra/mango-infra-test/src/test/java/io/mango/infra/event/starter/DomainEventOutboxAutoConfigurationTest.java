package io.mango.infra.event.starter;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventBus;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.event.api.command.ReconsumeSystemEventCommand;
import io.mango.infra.event.api.query.SystemEventPageQuery;
import io.mango.infra.event.core.outbox.OutboxDomainEventPublisher;
import io.mango.infra.event.core.system.SystemEventService;
import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxStatus;
import io.mango.infra.kv.starter.KvStoreAutoConfiguration;
import io.mango.infra.kv.starter.OutboxAutoConfiguration;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void whenSubscriberKeepsFailing_shouldMarkOutboxMessageFailedAndExposeSystemEvent() {
        runner.withPropertyValues(
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.outbox=true",
                        "mango.event.outbox.enabled=true",
                        "mango.event.outbox.worker-id=test-event-worker",
                        "mango.event.outbox.batch-size=10",
                        "mango.event.outbox.retry-delay-seconds=0",
                        "mango.event.outbox.max-attempts=2")
                .run(context -> {
                    IDomainEventBus eventBus = context.getBean(IDomainEventBus.class);
                    eventBus.subscribe("workflow.process.failed", event -> {
                        throw new IllegalStateException("consumer rejected");
                    });

                    IDomainEventPublisher publisher = context.getBean(IDomainEventPublisher.class);
                    DomainEvent event = DomainEvent.builder()
                            .eventType("workflow.process.failed")
                            .businessType("EXPENSE_REIMBURSEMENT")
                            .businessKey("EXP-20260516-FAILED")
                            .aggregateId("PROC-FAILED")
                            .build();
                    publisher.publish(event);

                    IOutboxDispatcher dispatcher = context.getBean(IOutboxDispatcher.class);
                    assertThat(dispatcher.dispatchOnce()).isEqualTo(0);
                    assertThat(dispatcher.dispatchOnce()).isEqualTo(0);

                    IOutboxStore store = context.getBean(IOutboxStore.class);
                    assertThat(store.findById(event.getEventId()).getStatus()).isEqualTo(OutboxStatus.FAILED);

                    SystemEventService service = context.getBean(SystemEventService.class);
                    SystemEventPageQuery query = new SystemEventPageQuery();
                    query.setKeyword("EXP-20260516-FAILED");
                    var page = service.page(query);
                    assertThat(page.getTotal()).isEqualTo(1L);
                    assertThat(page.getList().get(0).getStatus()).isEqualTo(OutboxStatus.FAILED);
                    assertThat(service.detail(event.getEventId()).getErrorMessage()).contains("consumer rejected");

                    ReconsumeSystemEventCommand command = new ReconsumeSystemEventCommand();
                    command.setMessageId(event.getEventId());
                    assertThat(service.reconsume(command)).isTrue();
                    assertThat(store.findById(event.getEventId()).getStatus()).isEqualTo(OutboxStatus.PENDING);
                });
    }

    @Test
    void whenRedisStreamTransportEnabled_shouldCreateTransportDispatcherAndConsumerScheduler() {
        runner.withPropertyValues(
                        "mango.kv.capability.enabled=true",
                        "mango.kv.capability.outbox=true",
                        "mango.event.outbox.enabled=true",
                        "mango.event.transport=redis-stream",
                        "mango.event.redis-stream.stream-name=mango:test:domain-event",
                        "mango.event.redis-stream.group=mango-test-domain-event",
                        "mango.event.redis-stream.consumer=mango-test-consumer")
                .withBean(RedissonClient.class, () -> {
                    RedissonClient redissonClient = mock(RedissonClient.class);
                    RStream<String, String> stream = mock(RStream.class);
                    when(redissonClient.<String, String>getStream(anyString(), any())).thenReturn(stream);
                    return redissonClient;
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(io.mango.infra.event.core.transport.DomainEventTransport.class);
                    assertThat(context).hasSingleBean(DomainEventTransportScheduler.class);
                    assertThat(context).hasSingleBean(IOutboxDispatcher.class);
                });
    }
}
