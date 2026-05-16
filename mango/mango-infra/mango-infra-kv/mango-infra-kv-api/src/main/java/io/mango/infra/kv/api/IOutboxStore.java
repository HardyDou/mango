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
     * Mark a message processed successfully.
     */
    void ack(String messageId, String workerId, Instant now);

    /**
     * Mark a message failed and schedule next retry.
     */
    void nack(String messageId, String workerId, String errorMessage, Instant nextAttemptAt, Instant now);
}
