package io.mango.auth.core.service;

import io.mango.auth.api.command.ChangeRequiredPasswordCommand;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LoginTenantOptionsCommand;
import io.mango.auth.api.command.WecomLoginCommand;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.api.vo.WecomLoginConfigVO;

import java.util.List;

public interface IAuthService {
    LoginVO login(LoginCommand command);

    LoginVO changeRequiredPassword(ChangeRequiredPasswordCommand command);

    LoginVO loginByWecom(WecomLoginCommand command);

    WecomLoginConfigVO getWecomLoginConfig(String tenantId);

    List<LoginTenantVO> listLoginTenants(LoginTenantOptionsCommand command);

    void logout(String token);

    LoginVO refreshToken(String refreshToken);

    boolean validateToken(String token);
}
