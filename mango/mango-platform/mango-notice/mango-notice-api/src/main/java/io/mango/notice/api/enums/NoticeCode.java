package io.mango.notice.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Notice module business result codes. */
@Getter
@AllArgsConstructor
public enum NoticeCode implements BizCode {

    /** Notice business precondition failed; detailed callers keep the original message. */
    NOTICE_BUSINESS_ERROR(400, "通知业务校验失败"),

    /** Requested site message is not visible to the current user. */
    NOTICE_SITE_MESSAGE_NOT_FOUND(404, "系统消息不存在");

    private final int code;
    private final String message;
}
