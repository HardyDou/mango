package io.mango.infra.event.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 事件模块业务错误码。
 *
 * <p>沿用历史前置条件的 400 错误码，只统一模块内校验契约。</p>
 */
@Getter
@AllArgsConstructor
public enum EventCode implements BizCode {

    /** 事件运维请求或运行参数不正确。 */
    EVENT_BUSINESS_ERROR(400, "事件业务校验失败");

    private final int code;
    private final String message;
}
