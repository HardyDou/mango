package io.mango.notice.api.enums;

public enum NoticeInboundMessageStatus {
    RECEIVED,
    ATTACHMENT_PROCESSING,
    READY_TO_BROADCAST,
    BROADCASTED,
    RETRYABLE_FAILED,
    DEAD_LETTER
}
