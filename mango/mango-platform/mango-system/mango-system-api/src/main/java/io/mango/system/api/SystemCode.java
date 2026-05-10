package io.mango.system.api;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统模块业务码。
 */
@Getter
@AllArgsConstructor
public enum SystemCode implements BizCode {

    /** 机构不存在。 */
    INSTITUTION_NOT_FOUND(2404, "机构不存在"),

    /** 机构状态非法。 */
    INSTITUTION_STATUS_INVALID(2408, "机构状态非法"),

    /** 机构存在关联数据，禁止删除。 */
    INSTITUTION_DELETE_BLOCKED(2409, "机构已初始化业务数据，请改为归档处理");

    private final int code;
    private final String message;
}
