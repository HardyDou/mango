package io.mango.auth.api;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证模块业务码。
 */
@Getter
@AllArgsConstructor
public enum AuthCode implements BizCode {

    /** 用户名或密码错误。 */
    LOGIN_ACCOUNT_OR_PASSWORD_INVALID(1400, "用户名或密码错误"),

    /** 登录时未选择机构。 */
    INSTITUTION_REQUIRED(1401, "请选择机构"),

    /** 机构服务不可用。 */
    INSTITUTION_PROVIDER_UNAVAILABLE(1501, "机构服务不可用"),

    /** 机构不存在、已停用或账号未加入机构。 */
    INSTITUTION_ACCESS_DENIED(1403, "机构不存在、已停用或当前账号未加入该机构"),

    /** 当前账号没有机构成员身份。 */
    INSTITUTION_MEMBER_REQUIRED(1404, "当前账号缺少机构成员身份"),

    /** 刷新令牌缺少机构上下文。 */
    REFRESH_TOKEN_INSTITUTION_CONTEXT_MISSING(1405, "刷新令牌缺少机构上下文"),

    /** 刷新令牌成员上下文与当前机构成员不一致。 */
    REFRESH_TOKEN_MEMBER_CONTEXT_MISMATCH(1406, "刷新令牌成员上下文不匹配"),

    /** 账号已停用。 */
    ACCOUNT_DISABLED(1407, "账号已停用"),

    /** 当前账号没有可登录机构。 */
    LOGIN_INSTITUTION_EMPTY(1408, "当前账号没有可登录机构"),

    /** 登录尝试过于频繁。 */
    LOGIN_ATTEMPT_LOCKED(1429, "登录尝试次数过多"),

    /** 刷新令牌无效或已过期。 */
    REFRESH_TOKEN_INVALID(1410, "登录已过期，请重新登录"),

    /** 当前访问令牌无效或已过期。 */
    ACCESS_TOKEN_INVALID(1411, "未登录或登录已过期"),

    /** 当前登录用户不存在。 */
    CURRENT_USER_NOT_FOUND(1412, "当前用户不存在"),

    /** 验证码服务不可用。 */
    CAPTCHA_SERVICE_UNAVAILABLE(1503, "验证码服务不可用"),

    /** 请求缺少验证码。 */
    CAPTCHA_REQUIRED(1428, "请先完成验证码"),

    /** 验证码校验失败。 */
    CAPTCHA_INVALID(1409, "验证码校验失败"),

    /** 防重放请求已过期。 */
    REQUEST_EXPIRED(1413, "请求已过期"),

    /** 强制改密凭据无效或已过期。 */
    PASSWORD_RESET_TICKET_INVALID(1417, "强制改密凭据无效或已过期"),

    /** 防重放时间戳非法。 */
    REQUEST_TIMESTAMP_INVALID(1414, "请求时间戳非法"),

    /** 重复请求。 */
    DUPLICATE_REQUEST(1415, "重复请求"),

    /** 请求签名非法。 */
    REQUEST_SIGNATURE_INVALID(1416, "请求签名非法");

    private final int code;
    private final String message;
}
