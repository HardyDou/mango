package io.mango.infra.realtime.starter.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.kv.api.IOutboxPublisher;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.IRealtimeReliablePublishService;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Map;

@RequiredArgsConstructor
public class RealtimeOutboxPublisher implements IRealtimeReliablePublishService {

    public static final String EVENT_TYPE = "realtime.message.dispatch";
    private static final String BUSINESS_TYPE = "realtime";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final IOutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(RealtimeOutboundMessage message) {
        outboxPublisher.publish(toOutboxMessage(message));
    }

    private OutboxMessage toOutboxMessage(RealtimeOutboundMessage message) {
        String targetType = message.resolvedTarget().type().name();
        String targetId = message.resolvedTarget().id();
        return OutboxMessage.builder()
                .messageId(message.id())
                .eventType(EVENT_TYPE)
                .businessType(BUSINESS_TYPE)
                .businessKey(message.id())
                .aggregateId(targetType + ":" + targetId)
                .occurredAt(Instant.now())
                .payload(objectMapper.convertValue(new RealtimeOutboxPayload(message), MAP_TYPE))
                .headers(Map.of(
                        "tenantId", message.tenantId(),
                        "targetType", targetType,
                        "targetId", targetId))
                .build();
    }
}
