package io.mango.identity.api;

import io.mango.identity.api.vo.AuthUserInfo;

/**
 * 认证用户事实本地 Provider。
 */
public interface AuthUserProvider {

    /** 按默认登录域查询认证用户事实。 */
    AuthUserInfo getByUsernameForAuth(String username);

    /** 按指定登录域查询认证用户事实。 */
    default AuthUserInfo getByUsernameForAuth(String username, String realm) {
        return getByUsernameForAuth(username);
    }

    /** 按用户 ID 查询认证用户事实。 */
    AuthUserInfo getByIdForAuth(Long userId);
}
