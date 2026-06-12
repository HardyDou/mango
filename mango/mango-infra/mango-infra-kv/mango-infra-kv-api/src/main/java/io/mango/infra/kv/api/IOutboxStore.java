package io.mango.infra.kv.api;

import java.time.Instant;
import java.util.List;

/**
 * Outbox persistence abstraction.
 */
public interface IOutboxStore {

    /**
     * Enqueue a message.
     */
    void enqueue(OutboxMessage message);

    /**
     * Claim a batch of ready messages for processing.
     */
    List<OutboxMessage> claim(String workerId, int batchSize, Instant now);

    /**
     * Claim a batch of ready messages for one event type.
     */
    default List<OutboxMessage> claim(String workerId, String eventType, int batchSize, Instant now) {
        return claim(workerId, batchSize, now).stream()
                .filter(message -> eventType != null && eventType.equals(message.getEventType()))
                .toList();
    }

    /**
     * Mark a message processed successfully.
     */
    void ack(String messageId, String workerId, Instant now);

    /**
     * Mark a message failed and schedule next retry.
     */
    void nack(String messageId, String workerId, String errorMessage, Instant nextAttemptAt, Instant now);

    /**
     * Mark a message as final failed.
     */
    default void fail(String messageId, String workerId, String errorMessage, Instant now) {
        nack(messageId, workerId, errorMessage, now, now);
    }

    /**
     * Requeue one failed or delayed message.
     */
    default void requeue(String messageId, Instant nextAttemptAt, Instant now) {
        throw new UnsupportedOperationException("Outbox requeue is not supported by this store");
    }

    /**
     * Find one message by id.
     */
    default OutboxMessage findById(String messageId) {
        return null;
    }

    /**
     * Query messages.
     */
    default List<OutboxMessage> query(OutboxMessageQuery query) {
        return List.of();
    }

    /**
     * Count messages.
     */
    default long count(OutboxMessageQuery query) {
        return 0L;
    }
}
