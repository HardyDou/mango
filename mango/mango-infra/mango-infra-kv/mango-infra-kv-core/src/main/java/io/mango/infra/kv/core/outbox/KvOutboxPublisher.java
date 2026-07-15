package io.mango.infra.kv.core.outbox;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.infra.kv.api.IOutboxPublisher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import lombok.RequiredArgsConstructor;

/**
 * Default outbox publisher.
 */
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "The constructor intentionally retains the shared Outbox store dependency"))
public class KvOutboxPublisher implements IOutboxPublisher {

    private final IOutboxStore outboxStore;

    @Override
    public void publish(OutboxMessage message) {
        outboxStore.enqueue(message);
    }
}
