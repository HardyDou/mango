package io.mango.infra.event.core.outbox;

import io.mango.common.result.Require;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxTopics;

import java.util.HashMap;

/**
 * Maps domain events to KV outbox messages.
 */
final class OutboxDomainEventConverter {

    private OutboxDomainEventConverter() {
    }

    static OutboxMessage toOutboxMessage(DomainEvent event) {
        Require.notNull(event, "事件不能为空");
        var payload = event.getPayload();
        if (payload == null) {
            payload = new HashMap<>();
        }
        var headers = event.getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
        }
        return OutboxMessage.builder()
                .messageId(event.getEventId())
                .topic(OutboxTopics.DOMAIN_EVENT)
                .eventType(event.getEventType())
                .businessType(event.getBusinessType())
                .businessKey(event.getBusinessKey())
                .aggregateId(event.getAggregateId())
                .occurredAt(event.getOccurredAt())
                .payload(payload)
                .headers(headers)
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
