package io.mango.infra.event.api;

/**
 * 领域事件订阅者。
 */
public interface DomainEventSubscriber {

    /**
     * 订阅的事件类型。返回 * 表示订阅所有事件。
     */
    String eventType();

    /**
     * 处理事件。
     */
    void onEvent(DomainEvent event);
}
