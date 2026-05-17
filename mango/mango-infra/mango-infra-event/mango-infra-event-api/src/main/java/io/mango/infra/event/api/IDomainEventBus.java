package io.mango.infra.event.api;

/**
 * 领域事件总线。
 */
public interface IDomainEventBus extends IDomainEventPublisher {

    /**
     * 订阅事件。
     *
     * @param eventType 事件类型，* 表示订阅所有事件
     * @param handler 事件处理器
     * @return 订阅句柄，关闭后取消订阅
     */
    AutoCloseable subscribe(String eventType, DomainEventHandler handler);
}
