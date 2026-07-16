package io.mango.identity.api;

import io.mango.identity.api.vo.AuthUserVO;

/**
 * 认证用户事实本地 Provider。
 */
public interface AuthUserProvider {

    /** 按默认登录域查询认证用户事实。 */
    AuthUserVO getByUsernameForAuth(String username);

    /** 按指定登录域查询认证用户事实。 */
    default AuthUserVO getByUsernameForAuth(String username, String realm) {
        return getByUsernameForAuth(username);
    }

    /** 按用户 ID 查询认证用户事实。 */
    AuthUserVO getByIdForAuth(Long userId);
}
