package io.mango.authorization.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 授权模块业务码。
 */
@Getter
@AllArgsConstructor
public enum AuthorizationCode implements BizCode {

    /** 授权业务参数或前置条件不满足。 */
    AUTHORIZATION_BUSINESS_ERROR(400, "授权业务校验失败"),

    /** 授权资源不存在。 */
    AUTHORIZATION_NOT_FOUND(404, "授权资源不存在"),

    /** 当前主体无权执行授权操作。 */
    AUTHORIZATION_FORBIDDEN(403, "无权执行该授权操作"),

    /** 当前请求缺少有效登录上下文。 */
    AUTHORIZATION_UNAUTHORIZED(401, "缺少有效登录上下文");

    private final int code;
    private final String message;
}
