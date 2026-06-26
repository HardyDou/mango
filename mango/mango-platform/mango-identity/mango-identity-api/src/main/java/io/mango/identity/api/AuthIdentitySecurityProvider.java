package io.mango.identity.api;

import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.vo.AuthUserInfo;

/**
 * 认证链路身份安全状态 Provider。
 */
public interface AuthIdentitySecurityProvider {

    /**
     * 登录前检查账号安全状态。
     */
    void assertLoginAllowed(AuthUserInfo user);

    /**
     * 记录密码校验失败。
     */
    void recordLoginFailure(Long userId);

    /**
     * 记录登录成功。
     */
    void recordLoginSuccess(Long userId);

    /**
     * 使用强制改密凭据修改密码。
     */
    void changeRequiredPassword(ChangeRequiredPasswordCommand command);
}
