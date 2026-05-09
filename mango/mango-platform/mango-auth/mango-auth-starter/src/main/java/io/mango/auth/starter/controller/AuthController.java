package io.mango.auth.starter.controller;

import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LoginTenantOptionsCommand;
import io.mango.auth.api.command.LogoutCommand;
import io.mango.auth.api.command.RefreshTokenCommand;
import io.mango.auth.api.command.ValidateTokenCommand;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.core.service.impl.LoginAttemptTracker;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.starter.TtlExecutorDecorator;
import io.mango.infra.iplocation.api.IpLocationResolver;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.iplocation.api.IpLocation;
import io.mango.system.api.SysLoginLogApi;
import io.mango.system.api.po.SysLoginLogPo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 认证控制器，提供认证 HTTP 端点。
 * 具体认证逻辑委托给 {@link IAuthService}。
 *
 * @author Mango
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "认证授权", description = "认证登录、令牌刷新、退出登录接口")
public class AuthController {

    private final IAuthService authService;
    private final TtlExecutorDecorator ttlExecutorDecorator;
    private final ObjectProvider<IKvStore> kvStoreProvider;
    private final ITokenProvider tokenProvider;
    private final IdentityUserApi identityUserApi;
    private final IAuthorizationProvider authorizationProvider;
    private final ObjectProvider<CaptchaApi> captchaApiProvider;
    private final ObjectProvider<SysLoginLogApi> sysLoginLogApiProvider;
    private final ObjectProvider<IpLocationResolver> ipLocationResolverProvider;
    private LoginAttemptTracker loginAttemptTracker;
    private ScheduledExecutorService executorForTracker;

    @Autowired
    public AuthController(IAuthService authService,
                          TtlExecutorDecorator ttlExecutorDecorator,
                          ObjectProvider<IKvStore> kvStoreProvider,
                          ITokenProvider tokenProvider,
                          IdentityUserApi identityUserApi,
                          IAuthorizationProvider authorizationProvider,
                          ObjectProvider<CaptchaApi> captchaApiProvider,
                          ObjectProvider<SysLoginLogApi> sysLoginLogApiProvider,
                          ObjectProvider<IpLocationResolver> ipLocationResolverProvider) {
        this.authService = authService;
        this.ttlExecutorDecorator = ttlExecutorDecorator;
        this.kvStoreProvider = kvStoreProvider;
        this.tokenProvider = tokenProvider;
        this.identityUserApi = identityUserApi;
        this.authorizationProvider = authorizationProvider;
        this.captchaApiProvider = captchaApiProvider;
        this.sysLoginLogApiProvider = sysLoginLogApiProvider;
        this.ipLocationResolverProvider = ipLocationResolverProvider;
    }

    @PostConstruct
    public void init() {
        this.executorForTracker = ttlExecutorDecorator.decorate(
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "login-attempt-cleanup");
                t.setDaemon(true);
                return t;
            })
        );
        this.loginAttemptTracker = new LoginAttemptTracker(kvStoreProvider.getIfAvailable(), executorForTracker);
    }

    @PreDestroy
    public void shutdown() {
        if (loginAttemptTracker != null) {
            loginAttemptTracker.shutdown();
        }
        if (executorForTracker != null) {
            executorForTracker.shutdown();
            try {
                if (!executorForTracker.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorForTracker.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorForTracker.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @PostMapping("/login")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "用户登录")
    @Operation(summary = "用户登录", description = "公开接口。使用用户名、密码、登录域和验证码信息登录，成功后返回访问令牌、刷新令牌和用户授权信息")
    public R<LoginVO> loginEndpoint(
            @Valid @RequestBody LoginCommand loginCommand,
            HttpServletRequest request,
            HttpServletResponse response) {
        String clientIp = resolveClientIp(request);
        R<LoginVO> result = doLogin(loginCommand, clientIp);
        if (result.isSuccess() && result.getData() != null) {
            Cookie cookie = new Cookie("MANGO_TOKEN", result.getData().getAccessToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
        }
        recordLoginLog(loginCommand, request, result, clientIp);
        return result;
    }

    @PostMapping("/login-institutions")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "查询账号可登录机构")
    @Operation(summary = "查询账号可登录机构", description = "公开接口。校验用户名、密码和登录域后，返回当前账号可进入的启用机构列表，用于登录页机构选择")
    public R<List<LoginTenantVO>> loginInstitutions(@Valid @RequestBody LoginTenantOptionsCommand command) {
        return R.ok(authService.listLoginTenants(command));
    }

    private R<LoginVO> doLogin(LoginCommand loginCommand, String clientIp) {
        String loginKey = loginCommand.getUsername() + ":" + clientIp;

        if (loginAttemptTracker.isLockedOut(loginKey)) {
            long remainingMinutes = loginAttemptTracker.getRemainingLockoutMinutes(loginKey);
            log.warn("Login attempt blocked for {} - locked out for {} more minutes", loginKey, remainingMinutes);
            return R.fail(429, "登录尝试次数过多，请在 " + remainingMinutes + " 分钟后重试");
        }

        try {
            LoginVO loginResponse = authService.login(loginCommand);
            if (loginResponse == null) {
                throw new SecurityException("Invalid username or password");
            }
            loginAttemptTracker.clearAttempts(loginKey);
            return R.ok(loginResponse);
        } catch (SecurityException e) {
            loginAttemptTracker.recordFailedAttempt(loginKey);
            return R.fail(401, e.getMessage());
        }
    }

    private R<LoginVO> refreshToken(RefreshTokenCommand command) {
        try {
            LoginVO response = authService.refreshToken(command.getRefreshToken());
            if (response == null) {
                throw new SecurityException("Invalid or expired refresh token");
            }
            return R.ok(response);
        } catch (SecurityException e) {
            return R.fail(401, e.getMessage());
        }
    }

    @PostMapping("/refresh")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "刷新访问令牌")
    @Operation(summary = "刷新访问令牌", description = "公开接口。使用刷新令牌换取新的访问令牌")
    public R<LoginVO> refreshEndpoint(@Valid @RequestBody RefreshTokenCommand command) {
        return refreshToken(command);
    }

    @PostMapping("/logout")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "用户退出登录")
    @Operation(summary = "用户退出登录", description = "登录接口。退出当前登录状态并清理浏览器令牌 Cookie；令牌可通过请求体或 Authorization 请求头传入")
    public R<Void> logoutEndpoint(@Valid @RequestBody(required = false) LogoutCommand command,
                                  @Parameter(description = "访问令牌，格式为 Bearer <accessToken>")
                                  @RequestHeader(value = "Authorization", required = false) String token,
                                  HttpServletResponse response) {
        String resolvedToken = command != null && command.getToken() != null ? command.getToken() : token;
        authService.logout(resolvedToken);
        Cookie cookie = new Cookie("MANGO_TOKEN", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return R.ok();
    }

    private R<Boolean> validateToken(ValidateTokenCommand command) {
        return R.ok(authService.validateToken(command.getToken()));
    }

    @PostMapping("/validate")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "校验令牌")
    @Operation(summary = "校验访问令牌", description = "登录接口。校验访问令牌是否仍然有效")
    public R<Boolean> validateEndpoint(@Valid @RequestBody ValidateTokenCommand command) {
        return validateToken(command);
    }

    @GetMapping("/info")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取当前登录用户信息")
    @Operation(summary = "获取当前用户信息", description = "登录接口。根据 Authorization 请求头中的访问令牌返回当前用户资料、角色和权限")
    public R<LoginVO> info(
            @Parameter(description = "访问令牌，格式为 Bearer <accessToken>")
            @RequestHeader(value = "Authorization", required = false) String token) {
        String resolvedToken = stripBearer(token);
        Long userId = tokenProvider.getUserId(resolvedToken);
        if (userId == null) {
            return R.fail(401, "未登录");
        }
        IdentityUserInfo userInfo = identityUserApi.getUserInfoById(userId).getData();
        if (userInfo == null) {
            return R.fail(404, "用户不存在");
        }
        LoginVO vo = new LoginVO();
        vo.setUserId(userInfo.getUserId());
        vo.setMemberId(parseLong(tokenProvider.getClaim(resolvedToken, "memberId")));
        vo.setUsername(userInfo.getUsername());
        vo.setNickname(userInfo.getNickname());
        vo.setRealm(userInfo.getRealm());
        vo.setActorType(userInfo.getActorType());
        vo.setPartyType(userInfo.getPartyType());
        vo.setPartyId(userInfo.getPartyId());
        String appCode = tokenProvider.getClaim(resolvedToken, "appCode");
        String tenantId = tokenProvider.getClaim(resolvedToken, "tenantId");
        String tenantCode = tokenProvider.getClaim(resolvedToken, "tenantCode");
        vo.setTenantId(tenantId);
        vo.setTenantCode(tenantCode);
        vo.setTenantName(tokenProvider.getClaim(resolvedToken, "tenantName"));
        vo.setAppCode(appCode);
        Long memberId = vo.getMemberId();
        if (memberId == null) {
            return R.fail(401, "令牌缺少租户成员身份");
        }
        var snapshot = authorizationProvider.load(AuthorizationQuery.member(memberId)
                .withTenantId(tenantId)
                .withSystemCode(appCode)
                .withRealm(vo.getRealm())
                .withActorType(vo.getActorType())
                .withParty(vo.getPartyType(), vo.getPartyId()));
        vo.setRoles(snapshot.roleCodes().stream().toList());
        vo.setPermissions(snapshot.permissionCodes().stream().toList());
        return R.ok(vo);
    }

    @PostMapping("/captcha/send")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "发送短信或邮件验证码")
    @Operation(summary = "发送登录验证码", description = "公开接口。发送短信或邮件验证码，用于登录、注册、找回密码等业务场景")
    public R<String> sendCaptcha(@Valid @RequestBody CaptchaSendRequest request) {
        CaptchaApi captchaApi = captchaApiProvider.getIfAvailable();
        if (captchaApi == null) {
            return R.fail(503, "验证码服务不可用");
        }
        return R.ok(captchaApi.send(request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String clientIp = MangoContextHolder.clientIp();
        if (clientIp != null) {
            return clientIp;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.contains(",") ? forwardedFor.substring(0, forwardedFor.indexOf(',')).trim() : forwardedFor.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
    }

    private void recordLoginLog(LoginCommand command, HttpServletRequest request, R<LoginVO> result, String clientIp) {
        SysLoginLogApi logApi = sysLoginLogApiProvider.getIfAvailable();
        if (logApi == null) {
            return;
        }
        try {
            LoginVO login = result.getData();
            SysLoginLogPo log = new SysLoginLogPo();
            log.setTenantId(parseLong(login != null ? login.getTenantId() : command.getTenantId()));
            log.setUserId(login != null ? login.getUserId() : null);
            log.setUsername(command.getUsername());
            log.setLoginType(firstText(command.getRealm(), login != null ? login.getRealm() : null, "PASSWORD"));
            log.setIp(clientIp);
            log.setLocation(resolveLocation(clientIp));
            log.setBrowser(truncate(firstText(request.getHeader("User-Agent"), "未知"), 100));
            log.setOs("未知");
            log.setStatus(result.isSuccess() ? 1 : 0);
            log.setMsg(result.getMsg());
            log.setLoginTime(LocalDateTime.now());
            logApi.record(log);
        } catch (Exception e) {
            log.warn("Failed to record login log for {}", command.getUsername(), e);
        }
    }

    private String resolveLocation(String clientIp) {
        IpLocationResolver resolver = ipLocationResolverProvider.getIfAvailable();
        if (resolver == null) {
            return "未知";
        }
        IpLocation location = resolver.resolve(clientIp);
        return location == null ? "未知" : location.displayText();
    }

    private String stripBearer(String token) {
        if (token == null) {
            return null;
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
