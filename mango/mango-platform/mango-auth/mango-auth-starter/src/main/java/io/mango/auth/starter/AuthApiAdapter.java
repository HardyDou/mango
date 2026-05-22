package io.mango.auth.starter;

import io.mango.auth.api.AuthCode;
import io.mango.auth.api.AuthApi;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LogoutCommand;
import io.mango.auth.api.command.RefreshTokenCommand;
import io.mango.auth.api.command.ValidateTokenCommand;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.core.service.IAuthService;
import io.mango.common.result.Require;
import io.mango.common.result.R;
import lombok.RequiredArgsConstructor;

/**
 * 认证本地 API 适配器。
 * <p>
 * 用于进程内调用 {@link AuthApi} 契约，HTTP 入口由 Controller 单独承载。
 */
@RequiredArgsConstructor
public class AuthApiAdapter implements AuthApi {

    private final IAuthService authService;

    @Override
    public R<LoginVO> login(LoginCommand loginCommand) {
        LoginVO response = authService.login(loginCommand);
        Require.notNull(response, AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        return R.ok(response);
    }

    @Override
    public R<LoginVO> refreshToken(RefreshTokenCommand command) {
        LoginVO response = authService.refreshToken(command.getRefreshToken());
        Require.notNull(response, AuthCode.REFRESH_TOKEN_INVALID);
        return R.ok(response);
    }

    @Override
    public R<Void> logout(LogoutCommand command) {
        authService.logout(command.getToken());
        return R.ok();
    }

    @Override
    public R<Boolean> validateToken(ValidateTokenCommand command) {
        return R.ok(authService.validateToken(command.getToken()));
    }
}
