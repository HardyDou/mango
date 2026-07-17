package io.mango.link.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 网址导航模块业务码。 */
@Getter
@AllArgsConstructor
public enum LinkCode implements BizCode {

    /** 网址导航业务前置条件不满足。 */
    LINK_BUSINESS_ERROR(400, "网址导航业务校验失败");

    private final int code;
    private final String message;
}
