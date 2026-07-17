package io.mango.home.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 首页工作台模块业务码。 */
@Getter
@AllArgsConstructor
public enum HomeCode implements BizCode {

    /** 首页工作台业务前置条件不满足。 */
    HOME_BUSINESS_ERROR(400, "首页工作台业务校验失败"),

    /** 首页或模板不存在。 */
    HOME_NOT_FOUND(404, "首页或模板不存在");

    private final int code;
    private final String message;
}
