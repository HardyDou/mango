package io.mango.auth.api;

import io.mango.auth.api.command.ChangeRequiredPasswordCommand;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LoginTenantOptionsCommand;
import io.mango.auth.api.command.LogoutCommand;
import io.mango.auth.api.command.RefreshTokenCommand;
import io.mango.auth.api.command.SendAuthCaptchaCommand;
import io.mango.auth.api.command.ValidateTokenCommand;
import io.mango.auth.api.command.WecomLoginCommand;
import io.mango.auth.api.command.SaveProviderConfigCommand;
import io.mango.auth.api.command.StartProviderAuthorizationCommand;
import io.mango.auth.api.command.CompleteProviderAuthorizationCommand;
import io.mango.auth.api.command.BindExistingAccountCommand;
import io.mango.auth.api.vo.AvailableProviderVO;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.api.vo.WecomLoginConfigVO;
import io.mango.auth.api.vo.ProviderConfigVO;
import io.mango.auth.api.vo.ProviderAuthorizationVO;
import io.mango.auth.api.vo.ProviderAuthorizationResultVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 认证 API 契约。
 * 本地由 Controller 暴露，远程由 Feign Client 暴露。
 *
 * @author Mango
 */
@Validated
public interface AuthApi {

    R<List<ProviderConfigVO>> listProviderConfigs(@NotBlank @Size(max = 64) String appCode);

    R<ProviderConfigVO> createProviderConfig(@Valid SaveProviderConfigCommand command);

    R<ProviderConfigVO> updateProviderConfig(@Valid SaveProviderConfigCommand command);

    R<List<AvailableProviderVO>> listAvailableProviders(
            @NotBlank @Size(max = 64) String tenantId,
            @NotBlank @Size(max = 64) String appCode);

    R<ProviderAuthorizationVO> startProviderAuthorization(@Valid StartProviderAuthorizationCommand command);

    R<ProviderAuthorizationResultVO> completeProviderAuthorization(
            @Valid CompleteProviderAuthorizationCommand command);

    R<LoginVO> bindExistingProviderAccount(@Valid BindExistingAccountCommand command);

    /**
     * 用户登录。
     *
     * @param loginCommand 登录命令
     * @return 登录结果，包含访问令牌
     */
    R<LoginVO> login(@Valid LoginCommand loginCommand);

    R<List<LoginTenantVO>> loginInstitutions(@Valid LoginTenantOptionsCommand command);

    R<LoginVO> wecomLogin(@Valid WecomLoginCommand command);

    R<WecomLoginConfigVO> wecomLoginConfig(@NotBlank @Size(max = 64) String tenantId);

    R<LoginVO> changeRequiredPassword(@Valid ChangeRequiredPasswordCommand command);

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

    R<LoginVO> info(@Size(max = 4096) String authorization);

    R<String> sendCaptcha(@Valid SendAuthCaptchaCommand command);
}
