package io.mango.infra.event.core.transport;

import io.mango.infra.event.api.DomainEvent;

/**
 * Cross-process domain event transport.
 */
public interface DomainEventTransport {

    /**
     * Publish one event to the transport.
     */
    void publish(DomainEvent event);

    /**
     * Consume and dispatch a batch from the transport.
     */
    int consumeOnce();
}
