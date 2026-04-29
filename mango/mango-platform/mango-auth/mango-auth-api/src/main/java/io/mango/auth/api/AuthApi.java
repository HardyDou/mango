package io.mango.auth.api;

import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LogoutCommand;
import io.mango.auth.api.command.RefreshTokenCommand;
import io.mango.auth.api.command.ValidateTokenCommand;
import io.mango.auth.api.vo.LoginVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

/**
 * 认证 API 契约。
 * 本地由 Controller 暴露，远程由 Feign Client 暴露。
 *
 * @author Mango
 */
@Validated
public interface AuthApi {

    /**
     * 用户登录。
     *
     * @param loginCommand 登录命令
     * @return 登录结果，包含访问令牌
     */
    R<LoginVO> login(@Valid LoginCommand loginCommand);

    /**
     * 刷新令牌。
     *
     * @param command 刷新令牌命令
     * @return 新登录结果，包含新的访问令牌
     */
    R<LoginVO> refreshToken(@Valid RefreshTokenCommand command);

    /**
     * 用户退出登录。
     *
     * @param command 退出登录命令
     */
    R<Void> logout(@Valid LogoutCommand command);

    /**
     * 校验令牌。
     *
     * @param command 令牌校验命令
     * @return 令牌是否有效
     */
    R<Boolean> validateToken(@Valid ValidateTokenCommand command);
}
