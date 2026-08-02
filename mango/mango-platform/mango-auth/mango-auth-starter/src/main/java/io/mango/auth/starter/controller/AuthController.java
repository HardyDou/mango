package io.mango.auth.starter.controller;

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
import io.mango.auth.core.service.IAuthProviderConfigService;
import io.mango.auth.core.service.IExternalAuthorizationService;
import io.mango.auth.core.service.IAuthService;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.infra.context.api.MangoContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 认证 HTTP 协议适配器。
 */
@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证授权", description = "认证登录、令牌刷新、退出登录接口")
public class AuthController implements AuthApi {

    private final IAuthService authService;
    private final IAuthProviderConfigService authProviderConfigService;
    private final IExternalAuthorizationService externalAuthorizationService;

    @Override
    @GetMapping("/provider-configs")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "auth:provider-config:view")
    @Operation(summary = "查询第三方登录配置")
    public R<List<ProviderConfigVO>> listProviderConfigs(@RequestParam("appCode") String appCode) {
        return R.ok(authProviderConfigService.listCurrentTenant(appCode));
    }

    @Override
    @PostMapping("/provider-configs")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "auth:provider-config:edit")
    @Operation(summary = "创建第三方登录配置")
    public R<ProviderConfigVO> createProviderConfig(@RequestBody SaveProviderConfigCommand command) {
        command.setId(null);
        return R.ok(authProviderConfigService.save(command));
    }

    @Override
    @PutMapping("/provider-configs")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "auth:provider-config:edit")
    @Operation(summary = "更新第三方登录配置")
    public R<ProviderConfigVO> updateProviderConfig(@RequestBody SaveProviderConfigCommand command) {
        return R.ok(authProviderConfigService.save(command));
    }

    @Override
    @GetMapping("/providers")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "查询可用第三方登录方式")
    @Operation(summary = "查询可用第三方登录方式")
    public R<List<AvailableProviderVO>> listAvailableProviders(
            @RequestParam("tenantId") String tenantId,
            @RequestParam("appCode") String appCode) {
        return R.ok(authProviderConfigService.listAvailable(tenantId, appCode));
    }

    @Override
    @PostMapping("/providers/authorize")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "发起第三方授权")
    @Operation(summary = "发起第三方授权")
    public R<ProviderAuthorizationVO> startProviderAuthorization(
            @RequestBody StartProviderAuthorizationCommand command) {
        return R.ok(externalAuthorizationService.start(command));
    }

    @Override
    @PostMapping("/providers/complete")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "完成第三方授权")
    @Operation(summary = "完成第三方授权")
    public R<ProviderAuthorizationResultVO> completeProviderAuthorization(
            @RequestBody CompleteProviderAuthorizationCommand command) {
        return R.ok(externalAuthorizationService.complete(command));
    }

    @Override
    @PostMapping("/providers/bind-existing")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "绑定已有 Mango 账号")
    @Operation(summary = "绑定已有 Mango 账号")
    public R<LoginVO> bindExistingProviderAccount(@RequestBody BindExistingAccountCommand command) {
        return R.ok(externalAuthorizationService.bindExisting(command));
    }

    @Override
    @PostMapping("/login")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "用户登录")
    @Operation(summary = "用户登录", description = "使用用户名、密码、机构和验证码登录")
    public R<LoginVO> login(@RequestBody LoginCommand command) {
        enrichClientContext(command);
        return R.ok(authService.login(command));
    }

    @Override
    @PostMapping("/login-institutions")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "查询账号可登录机构")
    @Operation(summary = "查询账号可登录机构", description = "校验用户名和登录域后返回当前账号可进入的启用机构")
    public R<List<LoginTenantVO>> loginInstitutions(@RequestBody LoginTenantOptionsCommand command) {
        return R.ok(authService.listLoginTenants(command));
    }

    @Override
    @PostMapping("/wecom/login")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "企业微信扫码登录")
    @Operation(summary = "企业微信扫码登录", description = "使用企业微信授权码换取已绑定用户的登录令牌")
    public R<LoginVO> wecomLogin(@RequestBody WecomLoginCommand command) {
        return R.ok(authService.loginByWecom(command));
    }

    @Override
    @GetMapping("/wecom/login-config")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "查询企业微信扫码登录配置")
    @Operation(summary = "查询企业微信扫码登录配置", description = "按机构读取启用的企业微信扫码登录公开配置")
    public R<WecomLoginConfigVO> wecomLoginConfig(
            @Parameter(description = "机构ID", required = true)
            @RequestParam("tenantId") String tenantId) {
        return R.ok(authService.getWecomLoginConfig(tenantId));
    }

    @Override
    @PostMapping("/password/change-required")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "强制修改初始密码")
    @Operation(summary = "强制修改初始密码", description = "使用一次性强制改密凭据修改密码并签发正式令牌")
    public R<LoginVO> changeRequiredPassword(@RequestBody ChangeRequiredPasswordCommand command) {
        return R.ok(authService.changeRequiredPassword(command));
    }

    @Override
    @PostMapping("/refresh")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "刷新访问令牌")
    @Operation(summary = "刷新访问令牌", description = "使用刷新令牌换取新的访问令牌并撤销旧刷新令牌")
    public R<LoginVO> refreshToken(@RequestBody RefreshTokenCommand command) {
        return R.ok(authService.refreshToken(command.getRefreshToken()));
    }

    @Override
    @PostMapping("/logout")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "用户退出登录")
    @Operation(summary = "用户退出登录", description = "退出登录并清理浏览器令牌 Cookie；兼容 Authorization 请求头")
    public R<Void> logout(@RequestBody LogoutCommand command) {
        authService.logout(command.getToken());
        return R.ok();
    }

    @Override
    @PostMapping("/validate")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "校验令牌")
    @Operation(summary = "校验访问令牌", description = "校验访问令牌签名、有效期和撤销状态")
    public R<Boolean> validateToken(@RequestBody ValidateTokenCommand command) {
        return R.ok(authService.validateToken(command.getToken()));
    }

    @Override
    @GetMapping("/info")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取当前登录用户信息")
    @Operation(summary = "获取当前用户信息", description = "根据访问令牌返回当前用户资料、角色、权限和按钮规则")
    public R<LoginVO> info(
            @Parameter(description = "访问令牌，格式为 Bearer <accessToken>")
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(authService.info(authorization));
    }

    @Override
    @PostMapping("/captcha/send")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "发送短信或邮件验证码")
    @Operation(summary = "发送登录验证码", description = "通过验证码服务发送短信或邮件验证码并返回验证码键")
    public R<String> sendCaptcha(@RequestBody SendAuthCaptchaCommand command) {
        return R.ok(authService.sendCaptcha(command));
    }

    private void enrichClientContext(LoginCommand command) {
        HttpServletRequest request = currentRequest();
        command.setClientIp(resolveClientIp(request));
        command.setUserAgent(truncate(request.getHeader("User-Agent"), ClientContextLimits.USER_AGENT_LENGTH));
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String contextIp = MangoContextHolder.clientIp();
        if (contextIp != null && !contextIp.isBlank()) {
            return contextIp;
        }
        String forwarded = firstHeaderValue(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
            return forwarded;
        }
        String realIp = trimToNull(request.getHeader("X-Real-IP"));
        return realIp == null ? request.getRemoteAddr() : realIp;
    }

    private String firstHeaderValue(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        int separator = normalized.indexOf(',');
        return separator < 0 ? normalized : normalized.substring(0, separator).trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static final class ClientContextLimits {
        private static final int USER_AGENT_LENGTH = 512;

        private ClientContextLimits() {
        }
    }
}
