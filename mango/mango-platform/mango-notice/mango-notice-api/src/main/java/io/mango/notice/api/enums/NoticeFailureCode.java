package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知发送失败码")
public enum NoticeFailureCode {
    CHANNEL_UNAVAILABLE,
    CHANNEL_DISABLED,
    CHANNEL_CONFIG_INVALID,
    TEMPLATE_INVALID,
    RECIPIENT_INVALID,
    PROVIDER_REJECTED,
    PROVIDER_TIMEOUT,
    PROVIDER_ERROR,
    RATE_LIMITED,
    SEND_EXCEPTION
}
