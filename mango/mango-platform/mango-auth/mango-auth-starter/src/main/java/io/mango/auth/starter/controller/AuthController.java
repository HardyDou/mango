package io.mango.auth.starter.controller;

import io.mango.auth.api.AuthApi;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LogoutCommand;
import io.mango.auth.api.command.RefreshTokenCommand;
import io.mango.auth.api.command.ValidateTokenCommand;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.core.service.impl.LoginAttemptTracker;
import io.mango.common.result.R;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
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
public class AuthController implements AuthApi {

    private final IAuthService authService;
    private final TtlExecutorDecorator ttlExecutorDecorator;
    private final ObjectProvider<IKvStore> kvStoreProvider;
    private LoginAttemptTracker loginAttemptTracker;
    private ScheduledExecutorService executorForTracker;

    @Autowired
    public AuthController(IAuthService authService,
                          TtlExecutorDecorator ttlExecutorDecorator,
                          ObjectProvider<IKvStore> kvStoreProvider) {
        this.authService = authService;
        this.ttlExecutorDecorator = ttlExecutorDecorator;
        this.kvStoreProvider = kvStoreProvider;
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
    public R<LoginVO> refreshEndpoint(@Valid @RequestBody RefreshTokenCommand command) {
        return refreshToken(command);
    }

    /**
     * Logout (HTTP endpoint)
     */
    @Override
    public R<Void> logout(LogoutCommand command) {
        authService.logout(command.getToken());
        return R.ok();
    }

    @PostMapping("/logout")
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
     * Validate token (HTTP endpoint)
     */
    @Override
    public R<Boolean> validateToken(ValidateTokenCommand command) {
        return R.ok(authService.validateToken(command.getToken()));
    }

    @PostMapping("/validate")
    public R<Boolean> validateEndpoint(@Valid @RequestBody ValidateTokenCommand command) {
        return validateToken(command);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String clientIp = MangoContextHolder.clientIp();
        if (clientIp != null) {
            return clientIp;
        }
        return request.getRemoteAddr();
    }
}
