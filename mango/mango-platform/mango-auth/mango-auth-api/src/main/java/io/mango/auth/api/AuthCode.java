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
    LOGIN_INSTITUTION_EMPTY(1408, "当前账号没有可登录机构");

    private final int code;
    private final String message;
}
