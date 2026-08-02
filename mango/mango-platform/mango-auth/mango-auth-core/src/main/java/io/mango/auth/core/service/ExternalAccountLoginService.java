package io.mango.auth.core.service;

import io.mango.auth.api.vo.LoginVO;
import io.mango.identity.api.vo.AuthUserVO;

public interface ExternalAccountLoginService {

    AuthUserVO verifyBindingAccount(BindingCredentials credentials);

    LoginVO loginExternalUser(ExternalLoginContext context);

    record BindingCredentials(String username, String password, String tenantId) {
    }

    record ExternalLoginContext(Long userId, String tenantId, String appCode) {
    }
}
