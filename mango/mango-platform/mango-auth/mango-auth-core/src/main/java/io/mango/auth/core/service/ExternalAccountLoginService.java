package io.mango.auth.core.service;

import io.mango.auth.api.vo.LoginVO;
import io.mango.identity.api.vo.AuthUserVO;

public interface ExternalAccountLoginService {

    AuthUserVO verifyBindingAccount(String username, String password, String tenantId);

    LoginVO loginExternalUser(Long userId, String tenantId, String appCode);
}
