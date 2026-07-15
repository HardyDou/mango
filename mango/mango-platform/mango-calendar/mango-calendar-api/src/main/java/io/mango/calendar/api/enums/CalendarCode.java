package io.mango.calendar.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日历模块业务错误码。
 *
 * <p>历史日历前置条件统一返回 400；各调用点继续保留原有细化消息，保证 HTTP
 * 错误码和业务提示不变。</p>
 */
@Getter
@AllArgsConstructor
public enum CalendarCode implements BizCode {

    /** 日历业务前置条件不满足。 */
    CALENDAR_BUSINESS_ERROR(400, "日历业务校验失败");

    private final int code;
    private final String message;
}
