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
    private final int maxAttempts;

    public OutboxDomainEventDispatcher(
            IOutboxStore outboxStore,
            IDomainEventBus eventBus,
            Clock clock,
            String workerId,
            int batchSize,
            long retryDelaySeconds) {
        this(outboxStore, eventBus, clock, workerId, batchSize, retryDelaySeconds, 5);
    }

    public OutboxDomainEventDispatcher(
            IOutboxStore outboxStore,
            IDomainEventBus eventBus,
            Clock clock,
            String workerId,
            int batchSize,
            long retryDelaySeconds,
            int maxAttempts) {
        Require.notNull(outboxStore, "Outbox 存储不能为空");
        Require.notNull(eventBus, "事件总线不能为空");
        Require.notNull(clock, "时钟不能为空");
        Require.notBlank(workerId, "Outbox workerId 不能为空");
        Require.isTrue(retryDelaySeconds >= 0, "Outbox 重试延迟不能为负数");
        Require.isTrue(maxAttempts > 0, "Outbox 最大重试次数必须大于 0");
        this.outboxStore = outboxStore;
        this.eventBus = eventBus;
        this.clock = clock;
        this.workerId = workerId.trim();
        this.batchSize = batchSize;
        this.retryDelaySeconds = retryDelaySeconds;
        this.maxAttempts = maxAttempts;
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
                Instant failedAt = clock.instant();
                if (message.getAttemptCount() >= maxAttempts) {
                    outboxStore.fail(message.getMessageId(), workerId, ex.getMessage(), failedAt);
                } else {
                    outboxStore.nack(
                            message.getMessageId(),
                            workerId,
                            ex.getMessage(),
                            failedAt.plusSeconds(retryDelaySeconds),
                            failedAt);
                }
            }
        }
        return handled;
    }
}
