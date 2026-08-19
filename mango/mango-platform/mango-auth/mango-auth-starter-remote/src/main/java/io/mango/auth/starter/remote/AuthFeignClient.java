package io.mango.auth.starter.remote;

import io.mango.auth.api.AuthApi;
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
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 认证远程 HTTP 适配器。
 */
@FeignClient(name = "mango-auth", contextId = "authFeignClient", path = "/auth")
public interface AuthFeignClient extends AuthApi {

    @Override
    @GetMapping("/provider-configs")
    R<List<ProviderConfigVO>> listProviderConfigs(@RequestParam("appCode") String appCode);

    @Override
    @PostMapping("/provider-configs")
    R<ProviderConfigVO> createProviderConfig(@RequestBody SaveProviderConfigCommand command);

    @Override
    @PutMapping("/provider-configs")
    R<ProviderConfigVO> updateProviderConfig(@RequestBody SaveProviderConfigCommand command);

    @Override
    @GetMapping("/providers")
    R<List<AvailableProviderVO>> listAvailableProviders(@RequestParam("tenantId") String tenantId,
                                                        @RequestParam("appCode") String appCode);

    @Override
    @PostMapping("/providers/authorize")
    R<ProviderAuthorizationVO> startProviderAuthorization(@RequestBody StartProviderAuthorizationCommand command);

    @Override
    @PostMapping("/providers/complete")
    R<ProviderAuthorizationResultVO> completeProviderAuthorization(
            @RequestBody CompleteProviderAuthorizationCommand command);

    @Override
    @PostMapping("/providers/bind-existing")
    R<LoginVO> bindExistingProviderAccount(@RequestBody BindExistingAccountCommand command);

    @Override
    @PostMapping("/providers/wecom/profile/refresh")
    R<Boolean> refreshCurrentWecomProfile();

    @Override
    @PostMapping("/login")
    R<LoginVO> login(@RequestBody LoginCommand command);

    @Override
    @PostMapping("/login-institutions")
    R<List<LoginTenantVO>> loginInstitutions(@RequestBody LoginTenantOptionsCommand command);

    @Override
    @PostMapping("/wecom/login")
    R<LoginVO> wecomLogin(@RequestBody WecomLoginCommand command);

    @Override
    @GetMapping("/wecom/login-config")
    R<WecomLoginConfigVO> wecomLoginConfig(@RequestParam("tenantId") String tenantId);

    @Override
    @PostMapping("/password/change-required")
    R<LoginVO> changeRequiredPassword(@RequestBody ChangeRequiredPasswordCommand command);

    @Override
    @PostMapping("/refresh")
    R<LoginVO> refreshToken(@RequestBody RefreshTokenCommand command);

    @Override
    @PostMapping("/logout")
    R<Void> logout(@RequestBody LogoutCommand command);

    @Override
    @PostMapping("/validate")
    R<Boolean> validateToken(@RequestBody ValidateTokenCommand command);

    @Override
    @GetMapping("/info")
    R<LoginVO> info(@RequestHeader(value = "Authorization", required = false) String authorization);

    @Override
    @PostMapping("/captcha/send")
    R<String> sendCaptcha(@RequestBody SendAuthCaptchaCommand command);
}
