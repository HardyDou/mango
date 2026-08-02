package io.mango.auth.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证模块业务码。
 */
@Getter
@AllArgsConstructor
public enum AuthCode implements BizCode {

    LOGIN_ACCOUNT_OR_PASSWORD_INVALID(1400, "用户名或密码错误"),
    INSTITUTION_REQUIRED(1401, "请选择机构"),
    INSTITUTION_PROVIDER_UNAVAILABLE(1501, "机构服务不可用"),
    INSTITUTION_ACCESS_DENIED(1403, "机构不存在、已停用或当前账号未加入该机构"),
    INSTITUTION_MEMBER_REQUIRED(1404, "当前账号缺少机构成员身份"),
    REFRESH_TOKEN_INSTITUTION_CONTEXT_MISSING(1405, "刷新令牌缺少机构上下文"),
    REFRESH_TOKEN_MEMBER_CONTEXT_MISMATCH(1406, "刷新令牌成员上下文不匹配"),
    ACCOUNT_DISABLED(1407, "账号已停用"),
    LOGIN_INSTITUTION_EMPTY(1408, "当前账号没有可登录机构"),
    CAPTCHA_INVALID(1409, "验证码校验失败"),
    REFRESH_TOKEN_INVALID(1410, "登录已过期，请重新登录"),
    ACCESS_TOKEN_INVALID(1411, "未登录或登录已过期"),
    CURRENT_USER_NOT_FOUND(1412, "当前用户不存在"),
    REQUEST_EXPIRED(1413, "请求已过期"),
    REQUEST_TIMESTAMP_INVALID(1414, "请求时间戳非法"),
    DUPLICATE_REQUEST(1415, "重复请求"),
    REQUEST_SIGNATURE_INVALID(1416, "请求签名非法"),
    PASSWORD_RESET_TICKET_INVALID(1417, "强制改密凭据无效或已过期"),
    CAPTCHA_REQUIRED(1428, "请先完成验证码"),
    LOGIN_ATTEMPT_LOCKED(1429, "登录尝试次数过多"),
    CAPTCHA_SERVICE_UNAVAILABLE(1503, "验证码服务不可用"),
    WECOM_ACCOUNT_UNBOUND(1404, "当前企业微信账号尚未绑定 Mango 用户，请联系管理员绑定后再登录"),
    WECOM_CONFIG_UNAVAILABLE(1501, "企业微信扫码登录配置不存在或未启用"),
    PROVIDER_CONFIG_UNAVAILABLE(1501, "第三方登录配置不存在或未启用"),
    PROVIDER_CONFIG_INVALID(1402, "第三方登录配置无效"),
    PROVIDER_CONFIG_CONFLICT(1403, "第三方登录配置已存在"),
    PROVIDER_SECRET_INVALID(1502, "第三方登录密钥不可用"),
    PROVIDER_STATE_INVALID(1418, "第三方授权状态无效或已过期"),
    PROVIDER_REDIRECT_INVALID(1419, "第三方授权回调地址无效"),
    EXTERNAL_AUTH_FAILED(1504, "第三方授权失败"),
    EXTERNAL_BINDING_CONFLICT(1420, "该第三方账号已绑定其他 Mango 账号"),
    EXTERNAL_BIND_TICKET_INVALID(1421, "第三方账号绑定凭据无效或已过期"),
    AUTH_REQUEST_INVALID(400, "认证请求参数非法");

    private final int code;
    private final String message;
}
