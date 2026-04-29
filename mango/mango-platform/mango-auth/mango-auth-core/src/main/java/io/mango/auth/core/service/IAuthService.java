package io.mango.auth.core.service;

import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.vo.LoginVO;

public interface IAuthService {
    LoginVO login(LoginCommand command);

    void logout(String token);

    LoginVO refreshToken(String refreshToken);

    boolean validateToken(String token);
}
