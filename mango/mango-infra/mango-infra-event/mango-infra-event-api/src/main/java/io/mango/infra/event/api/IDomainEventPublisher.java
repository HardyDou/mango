package io.mango.infra.event.api;

/**
 * 领域事件发布器。
 */
public interface IDomainEventPublisher {

    void publish(DomainEvent event);
}
