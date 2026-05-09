package io.mango.auth.core.service;

import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LoginTenantOptionsCommand;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.auth.api.vo.LoginVO;

import java.util.List;

public interface IAuthService {
    LoginVO login(LoginCommand command);

    List<LoginTenantVO> listLoginTenants(LoginTenantOptionsCommand command);

    void logout(String token);

    LoginVO refreshToken(String refreshToken);

    boolean validateToken(String token);
}
