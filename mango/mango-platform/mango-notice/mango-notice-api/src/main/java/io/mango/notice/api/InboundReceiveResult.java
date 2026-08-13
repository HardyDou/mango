package io.mango.notice.api;

public record InboundReceiveResult(Long messageId, String eventId, boolean duplicate, boolean accepted) {
}
