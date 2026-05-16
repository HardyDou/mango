package io.mango.infra.event.core.outbox;

import io.mango.common.result.Require;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.kv.api.IOutboxPublisher;

/**
 * Domain event publisher backed by KV outbox.
 */
public class OutboxDomainEventPublisher implements IDomainEventPublisher {

    private final IOutboxPublisher outboxPublisher;

    public OutboxDomainEventPublisher(IOutboxPublisher outboxPublisher) {
        Require.notNull(outboxPublisher, "Outbox 发布器不能为空");
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        Require.notNull(event, "事件不能为空");
        Require.notBlank(event.getEventType(), "事件类型不能为空");
        outboxPublisher.publish(OutboxDomainEventMapper.toOutboxMessage(event));
    }
}
