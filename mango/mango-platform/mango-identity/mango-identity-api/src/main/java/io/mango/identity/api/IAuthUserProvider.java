package io.mango.identity.api;

import io.mango.identity.api.vo.AuthUserInfo;

public interface IAuthUserProvider {
    AuthUserInfo getByUsernameForAuth(String username);

    AuthUserInfo getByIdForAuth(Long userId);
}
