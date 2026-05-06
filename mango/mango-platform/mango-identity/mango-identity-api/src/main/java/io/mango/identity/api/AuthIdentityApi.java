package io.mango.identity.api;

import io.mango.common.result.R;
import io.mango.identity.api.query.AuthUsernameQuery;
import io.mango.identity.api.vo.AuthUserInfo;

/**
 * 认证用户事实内部契约。
 */
public interface AuthIdentityApi {

    /** 按登录域和用户名查询认证用户事实。 */
    R<AuthUserInfo> getByUsernameForAuth(AuthUsernameQuery query);

    /** 按用户 ID 查询认证用户事实。 */
    R<AuthUserInfo> getByIdForAuth(Long userId);
}
