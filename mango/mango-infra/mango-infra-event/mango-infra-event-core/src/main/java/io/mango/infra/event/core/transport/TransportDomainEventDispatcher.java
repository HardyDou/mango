package io.mango.infra.event.core.transport;

import io.mango.common.result.Require;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Relays KV outbox messages to a cross-process event transport.
 */
public class TransportDomainEventDispatcher implements IOutboxDispatcher {

    private final IOutboxStore outboxStore;
    private final DomainEventTransport transport;
    private final Clock clock;
    private final String workerId;
    private final int batchSize;
    private final long retryDelaySeconds;
    private final int maxAttempts;

    public TransportDomainEventDispatcher(
            IOutboxStore outboxStore,
            DomainEventTransport transport,
            Clock clock,
            String workerId,
            int batchSize,
            long retryDelaySeconds,
            int maxAttempts) {
        Require.notNull(outboxStore, "Outbox 存储不能为空");
        Require.notNull(transport, "事件传输不能为空");
        Require.notNull(clock, "时钟不能为空");
        Require.notBlank(workerId, "Outbox workerId 不能为空");
        Require.isTrue(retryDelaySeconds >= 0, "Outbox 重试延迟不能为负数");
        Require.isTrue(maxAttempts > 0, "Outbox 最大重试次数必须大于 0");
        this.outboxStore = outboxStore;
        this.transport = transport;
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
                transport.publish(toDomainEvent(message));
                outboxStore.ack(message.getMessageId(), workerId, clock.instant());
                handled++;
            } catch (RuntimeException ex) {
                markFailedOrRetry(message, ex);
            }
        }
        return handled;
    }

    private DomainEvent toDomainEvent(OutboxMessage message) {
        DomainEvent.DomainEventBuilder builder = DomainEvent.builder()
                .eventId(message.getMessageId())
                .eventType(message.getEventType())
                .businessType(message.getBusinessType())
                .businessKey(message.getBusinessKey())
                .aggregateId(message.getAggregateId())
                .occurredAt(message.getOccurredAt());
        if (message.getPayload() != null) {
            builder.payload(message.getPayload());
        }
        if (message.getHeaders() != null) {
            builder.headers(message.getHeaders());
        }
        return builder.build();
    }

    private void markFailedOrRetry(OutboxMessage message, RuntimeException ex) {
        Instant failedAt = clock.instant();
        if (message.getAttemptCount() >= maxAttempts) {
            outboxStore.fail(message.getMessageId(), workerId, ex.getMessage(), failedAt);
            return;
        }
        outboxStore.nack(
                message.getMessageId(),
                workerId,
                ex.getMessage(),
                failedAt.plusSeconds(retryDelaySeconds),
                failedAt);
    }
}
