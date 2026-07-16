package io.mango.domain.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务域模块业务码。
 */
@Getter
@AllArgsConstructor
public enum DomainCode implements BizCode {

    /** 请求参数不正确。 */
    VALIDATION_ERROR(400, "业务域参数非法"),

    /** 业务域不存在。 */
    NOT_FOUND(400, "业务域不存在"),

    /** 业务域数据冲突。 */
    CONFLICT(400, "业务域数据冲突");

    private final int code;
    private final String message;
}
