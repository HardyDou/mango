package io.mango.auth.api;

import io.mango.auth.api.po.AuthUserInfo;

public interface IAuthUserProvider {
    AuthUserInfo getByUsernameForAuth(String username) ;

    AuthUserInfo getByIdForAuth(Long userId);
}
