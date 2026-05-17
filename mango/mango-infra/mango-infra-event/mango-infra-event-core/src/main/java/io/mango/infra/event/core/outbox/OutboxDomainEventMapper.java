package io.mango.infra.event.core.outbox;

import io.mango.common.result.Require;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.kv.api.OutboxMessage;

import java.util.HashMap;

/**
 * Maps domain events to KV outbox messages.
 */
final class OutboxDomainEventMapper {

    private OutboxDomainEventMapper() {
    }

    static OutboxMessage toOutboxMessage(DomainEvent event) {
        Require.notNull(event, "事件不能为空");
        return OutboxMessage.builder()
                .messageId(event.getEventId())
                .eventType(event.getEventType())
                .businessType(event.getBusinessType())
                .businessKey(event.getBusinessKey())
                .aggregateId(event.getAggregateId())
                .occurredAt(event.getOccurredAt())
                .payload(event.getPayload() == null ? new HashMap<>() : event.getPayload())
                .headers(event.getHeaders() == null ? new HashMap<>() : event.getHeaders())
                .build();
    }

    static DomainEvent toDomainEvent(OutboxMessage message) {
        Require.notNull(message, "Outbox 消息不能为空");
        DomainEvent.DomainEventBuilder builder = DomainEvent.builder()
                .eventId(message.getMessageId())
                .eventType(message.getEventType())
                .businessType(message.getBusinessType())
                .businessKey(message.getBusinessKey())
                .aggregateId(message.getAggregateId())
                .occurredAt(message.getOccurredAt());
        if (message.getPayload() != null) {
            builder.payload(message.getPayload());
        }
        if (message.getHeaders() != null) {
            builder.headers(message.getHeaders());
        }
        return builder.build();
    }
}
