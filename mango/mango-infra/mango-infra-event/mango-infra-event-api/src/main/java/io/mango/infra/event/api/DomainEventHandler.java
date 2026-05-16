package io.mango.infra.event.api;

/**
 * 领域事件处理器。
 */
@FunctionalInterface
public interface DomainEventHandler {

    void handle(DomainEvent event);
}
