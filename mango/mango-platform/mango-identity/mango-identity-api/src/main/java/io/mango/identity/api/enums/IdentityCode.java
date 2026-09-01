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
    CURRENT_PASSWORD_INVALID(1400, "当前密码错误"),
    CONTACT_CAPTCHA_INVALID(1401, "联系方式验证码无效"),
    CONTACT_CAPTCHA_UNAVAILABLE(1503, "验证码服务不可用"),
    LOGIN_LOCKED(1429, "账号已被临时锁定"),
    ACCOUNT_UNAVAILABLE(1408, "登录账号不可用"),
    RECOVERABLE_ACCOUNT(1409, "该账号对应已移出成员，请恢复原成员或修改登录账号"),
    MEMBER_NOT_RECOVERABLE(1410, "原成员当前不可恢复"),
    CONFIG_ERROR(500, "身份安全配置错误");

    private final int code;
    private final String message;
}
