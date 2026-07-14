package io.mango.notice.core.outbox;

import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxTopics;
import io.mango.infra.context.api.MangoContextHeaders;
import io.mango.infra.context.api.MangoContextHolder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class NoticeOutboxMessageFactory {

    public static final String EVENT_TYPE = "notice.send";

    private NoticeOutboxMessageFactory() {
    }

    public static OutboxMessage toOutboxMessage(Long taskId) {
        return toOutboxMessage(taskId, null);
    }

    public static OutboxMessage toOutboxMessage(Long taskId, Instant nextAttemptAt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        Map<String, String> headers = new HashMap<>();
        String tenantId = MangoContextHolder.tenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            headers.put(MangoContextHeaders.TENANT_ID, tenantId);
        }
        return OutboxMessage.builder()
                .topic(OutboxTopics.NOTICE)
                .eventType(EVENT_TYPE)
                .businessType("notice")
                .businessKey(String.valueOf(taskId))
                .aggregateId(String.valueOf(taskId))
                .nextAttemptAt(nextAttemptAt)
                .payload(payload)
                .headers(headers)
                .build();
    }
}
