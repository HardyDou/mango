package io.mango.infra.event.core.outbox;

import io.mango.common.result.Require;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventBus;
import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Dispatches KV outbox messages to the domain event bus.
 */
public class OutboxDomainEventDispatcher implements IOutboxDispatcher {

    private final IOutboxStore outboxStore;
    private final IDomainEventBus eventBus;
    private final Clock clock;
    private final String workerId;
    private final int batchSize;
    private final long retryDelaySeconds;

    public OutboxDomainEventDispatcher(
            IOutboxStore outboxStore,
            IDomainEventBus eventBus,
            Clock clock,
            String workerId,
            int batchSize,
            long retryDelaySeconds) {
        Require.notNull(outboxStore, "Outbox 存储不能为空");
        Require.notNull(eventBus, "事件总线不能为空");
        Require.notNull(clock, "时钟不能为空");
        Require.notBlank(workerId, "Outbox workerId 不能为空");
        Require.isTrue(retryDelaySeconds >= 0, "Outbox 重试延迟不能为负数");
        this.outboxStore = outboxStore;
        this.eventBus = eventBus;
        this.clock = clock;
        this.workerId = workerId.trim();
        this.batchSize = batchSize;
        this.retryDelaySeconds = retryDelaySeconds;
    }

    @Override
    public int dispatchOnce() {
        if (batchSize <= 0) {
            return 0;
        }
        Instant now = clock.instant();
        List<OutboxMessage> messages = outboxStore.claim(workerId, batchSize, now);
        int handled = 0;
        for (OutboxMessage message : messages) {
            try {
                DomainEvent event = OutboxDomainEventMapper.toDomainEvent(message);
                eventBus.publish(event);
                outboxStore.ack(message.getMessageId(), workerId, clock.instant());
                handled++;
            } catch (RuntimeException ex) {
                outboxStore.nack(
                        message.getMessageId(),
                        workerId,
                        ex.getMessage(),
                        clock.instant().plusSeconds(retryDelaySeconds),
                        clock.instant());
            }
        }
        return handled;
    }
}
