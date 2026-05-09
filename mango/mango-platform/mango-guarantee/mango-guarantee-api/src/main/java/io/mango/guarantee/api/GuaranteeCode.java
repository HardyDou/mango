package io.mango.guarantee.api;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 保函协同模块业务码。
 */
@Getter
@AllArgsConstructor
public enum GuaranteeCode implements BizCode {

    /** 当前请求缺少机构上下文。 */
    TENANT_CONTEXT_REQUIRED(3401, "缺少当前机构上下文"),

    /** 保函业务单不存在或当前机构不可见。 */
    CASE_NOT_FOUND(3404, "保函业务单不存在或当前机构不可见");

    private final int code;
    private final String message;
}
