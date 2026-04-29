package io.mango.auth.starter.remote;

import io.mango.auth.api.AuthApi;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LogoutCommand;
import io.mango.auth.api.command.RefreshTokenCommand;
import io.mango.auth.api.command.ValidateTokenCommand;
import io.mango.auth.api.vo.LoginVO;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Auth Feign client - implements AuthApi for remote calls
 *
 * @author Mango
 */
@FeignClient(name = "mango-auth", path = "/auth")
public interface AuthFeignClient extends AuthApi {

    @Override
    @PostMapping("/login")
    R<LoginVO> login(@RequestBody LoginCommand loginCommand);

    @Override
    @PostMapping("/refresh")
    R<LoginVO> refreshToken(@RequestBody RefreshTokenCommand command);

    @Override
    @PostMapping("/logout")
    R<Void> logout(@RequestBody LogoutCommand command);

    @Override
    @PostMapping("/validate")
    R<Boolean> validateToken(@RequestBody ValidateTokenCommand command);
}
