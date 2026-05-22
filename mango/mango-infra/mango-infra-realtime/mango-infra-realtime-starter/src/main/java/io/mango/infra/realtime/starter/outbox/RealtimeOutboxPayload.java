package io.mango.infra.realtime.starter.outbox;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;

public record RealtimeOutboxPayload(RealtimeOutboundMessage message) {
}
