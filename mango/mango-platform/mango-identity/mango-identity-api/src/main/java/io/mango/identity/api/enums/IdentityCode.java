package io.mango.identity.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 身份域业务码。 */
@Getter
@AllArgsConstructor
public enum IdentityCode implements BizCode {

    VALIDATION_ERROR(400, "身份参数非法"),
    NOT_FOUND(400, "身份数据不存在"),
    CONFLICT(400, "身份数据冲突"),
    LOGIN_LOCKED(1429, "账号已被临时锁定"),
    CONFIG_ERROR(500, "身份安全配置错误");

    private final int code;
    private final String message;
}
