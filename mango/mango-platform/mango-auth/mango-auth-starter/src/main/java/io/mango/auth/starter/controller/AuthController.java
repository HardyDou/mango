package io.mango.auth.starter.controller;

import io.mango.auth.api.AuthApi;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LogoutCommand;
import io.mango.auth.api.command.RefreshTokenCommand;
import io.mango.auth.api.command.ValidateTokenCommand;
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
import io.mango.infra.kv.api.IKvStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class AuthController implements AuthApi {

    private final IAuthService authService;
    private final TtlExecutorDecorator ttlExecutorDecorator;
    private final ObjectProvider<IKvStore> kvStoreProvider;
    private final ITokenProvider tokenProvider;
    private final IdentityUserApi identityUserApi;
    private final IAuthorizationProvider authorizationProvider;
    private final ObjectProvider<CaptchaApi> captchaApiProvider;
    private LoginAttemptTracker loginAttemptTracker;
    private ScheduledExecutorService executorForTracker;

    @Autowired
    public AuthController(IAuthService authService,
                          TtlExecutorDecorator ttlExecutorDecorator,
                          ObjectProvider<IKvStore> kvStoreProvider,
                          ITokenProvider tokenProvider,
                          IdentityUserApi identityUserApi,
                          IAuthorizationProvider authorizationProvider,
                          ObjectProvider<CaptchaApi> captchaApiProvider) {
        this.authService = authService;
        this.ttlExecutorDecorator = ttlExecutorDecorator;
        this.kvStoreProvider = kvStoreProvider;
        this.tokenProvider = tokenProvider;
        this.identityUserApi = identityUserApi;
        this.authorizationProvider = authorizationProvider;
        this.captchaApiProvider = captchaApiProvider;
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

    /**
     * 用户登录。
     */
    @Override
    public R<LoginVO> login(LoginCommand loginCommand) {
        return doLogin(loginCommand, "unknown");
    }

    @PostMapping("/login")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "用户登录")
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
        return result;
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

    @Override
    public R<LoginVO> refreshToken(RefreshTokenCommand command) {
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
    public R<LoginVO> refreshEndpoint(@Valid @RequestBody RefreshTokenCommand command) {
        return refreshToken(command);
    }

    /**
     * 用户退出登录。
     */
    @Override
    public R<Void> logout(LogoutCommand command) {
        authService.logout(command.getToken());
        return R.ok();
    }

    @PostMapping("/logout")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "用户退出登录")
    public R<Void> logoutEndpoint(@Valid @RequestBody(required = false) LogoutCommand command,
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

    /**
     * 校验令牌。
     */
    @Override
    public R<Boolean> validateToken(ValidateTokenCommand command) {
        return R.ok(authService.validateToken(command.getToken()));
    }

    @PostMapping("/validate")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "校验令牌")
    public R<Boolean> validateEndpoint(@Valid @RequestBody ValidateTokenCommand command) {
        return validateToken(command);
    }

    @GetMapping("/info")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取当前登录用户信息")
    public R<LoginVO> info(@RequestHeader(value = "Authorization", required = false) String token) {
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
        vo.setUsername(userInfo.getUsername());
        vo.setNickname(userInfo.getNickname());
        vo.setRealm(userInfo.getRealm());
        vo.setActorType(userInfo.getActorType());
        vo.setPartyType(userInfo.getPartyType());
        vo.setPartyId(userInfo.getPartyId());
        String appCode = tokenProvider.getClaim(resolvedToken, "appCode");
        vo.setAppCode(appCode);
        var snapshot = authorizationProvider.load(AuthorizationQuery.user(userId).withSystemCode(appCode));
        vo.setRoles(snapshot.roleCodes().stream().toList());
        vo.setPermissions(snapshot.permissionCodes().stream().toList());
        return R.ok(vo);
    }

    @PostMapping("/captcha/send")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "发送短信或邮件验证码")
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
        return request.getRemoteAddr();
    }

    private String stripBearer(String token) {
        if (token == null) {
            return null;
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
}
