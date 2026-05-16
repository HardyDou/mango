package io.mango.infra.kv.core.outbox;

import io.mango.infra.kv.api.IOutboxPublisher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import lombok.RequiredArgsConstructor;

/**
 * Default outbox publisher.
 */
@RequiredArgsConstructor
public class KvOutboxPublisher implements IOutboxPublisher {

    private final IOutboxStore outboxStore;

    @Override
    public void publish(OutboxMessage message) {
        outboxStore.enqueue(message);
    }
}
