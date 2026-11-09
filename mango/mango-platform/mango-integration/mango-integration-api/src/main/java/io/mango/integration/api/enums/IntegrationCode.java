package io.mango.integration.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 集成应用模块业务码。
 */
@Getter
@AllArgsConstructor
public enum IntegrationCode implements BizCode {

    /** 集成业务参数或前置条件不满足。 */
    INTEGRATION_BUSINESS_ERROR(400, "集成业务校验失败"),

    /** 集成资源不存在。 */
    INTEGRATION_NOT_FOUND(404, "集成资源不存在"),

    /** 缺少有效登录上下文。 */
    INTEGRATION_UNAUTHORIZED(401, "缺少有效登录上下文");

    private final int code;
    private final String message;
}
