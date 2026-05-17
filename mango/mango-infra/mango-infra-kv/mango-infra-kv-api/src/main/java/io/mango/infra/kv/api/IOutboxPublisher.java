package io.mango.infra.kv.api;

/**
 * Outbox enqueue gateway.
 */
public interface IOutboxPublisher {

    void publish(OutboxMessage message);
}
