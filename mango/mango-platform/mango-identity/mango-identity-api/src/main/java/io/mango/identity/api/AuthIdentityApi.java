package io.mango.identity.api;

import io.mango.common.result.R;
import io.mango.identity.api.vo.AuthUserInfo;

/**
 * 认证用户事实内部契约。
 */
public interface AuthIdentityApi {

    /** 按默认登录域查询认证用户事实。 */
    R<AuthUserInfo> getByUsernameForAuth(String username);

    /** 按指定登录域查询认证用户事实。 */
    R<AuthUserInfo> getByUsernameForAuth(String realm, String username);

    /** 按用户 ID 查询认证用户事实。 */
    R<AuthUserInfo> getByIdForAuth(Long userId);
}
