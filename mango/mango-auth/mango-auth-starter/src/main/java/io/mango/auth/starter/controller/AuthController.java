package io.mango.auth.starter.controller;

import io.mango.auth.api.AuthApi;
import io.mango.auth.api.vo.LoginRequest;
import io.mango.auth.api.vo.LoginResponse;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.core.service.impl.LoginAttemptTracker;
import io.mango.common.result.R;
import io.mango.infra.context.starter.TtlExecutorDecorator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Authentication controller - HTTP endpoints for auth operations.
 * Delegates to {@link AuthApi} implementation.
 *
 * @author Mango
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController implements AuthApi {

    private final IAuthService authService;
    private final TtlExecutorDecorator ttlExecutorDecorator;
    private LoginAttemptTracker loginAttemptTracker;
    private ScheduledExecutorService executorForTracker;

    @Autowired
    public AuthController(IAuthService authService, TtlExecutorDecorator ttlExecutorDecorator) {
        this.authService = authService;
        this.ttlExecutorDecorator = ttlExecutorDecorator;
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
        this.loginAttemptTracker = new LoginAttemptTracker(executorForTracker);
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
     * User login
     */
    public LoginResponse login(LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        if (response == null) {
            throw new SecurityException("Invalid username or password");
        }
        return response;
    }

    /**
     * Refresh token
     */
    public LoginResponse refreshToken(String refreshToken) {
        LoginResponse response = authService.refreshToken(refreshToken);
        if (response == null) {
            throw new SecurityException("Invalid or expired refresh token");
        }
        return response;
    }

    /**
     * User logout
     */
    public void logout(String token) {
        authService.logout(token);
    }

    /**
     * Validate token
     */
    public Boolean validateToken(String token) {
        return authService.validateToken(token);
    }

    /**
     * User login (HTTP endpoint)
     */
    @PostMapping("/login")
    public R<LoginResponse> loginEndpoint(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        // Get client IP for rate limiting
        String clientIp = getClientIp(request);
        String loginKey = loginRequest.getUsername() + ":" + clientIp;

        // Check if locked out
        if (loginAttemptTracker.isLockedOut(loginKey)) {
            long remainingMinutes = loginAttemptTracker.getRemainingLockoutMinutes(loginKey);
            log.warn("Login attempt blocked for {} - locked out for {} more minutes", loginKey, remainingMinutes);
            return R.fail(429, "登录尝试次数过多，请在 " + remainingMinutes + " 分钟后重试");
        }

        try {
            LoginResponse loginResponse = login(loginRequest);
            // Clear failed attempts on success
            loginAttemptTracker.clearAttempts(loginKey);
            // Set httpOnly Cookie for token
            Cookie cookie = new Cookie("MANGO_TOKEN", loginResponse.getAccessToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
            return R.ok(loginResponse);
        } catch (SecurityException e) {
            // Record failed attempt
            loginAttemptTracker.recordFailedAttempt(loginKey);
            return R.fail(401, e.getMessage());
        }
    }

    /**
     * Refresh token (HTTP endpoint)
     */
    @PostMapping("/refresh")
    public R<LoginResponse> refreshEndpoint(@RequestParam String refreshToken) {
        try {
            return R.ok(refreshToken(refreshToken));
        } catch (SecurityException e) {
            return R.fail(401, e.getMessage());
        }
    }

    /**
     * Logout (HTTP endpoint)
     */
    @PostMapping("/logout")
    public R<Void> logoutEndpoint(
            @RequestHeader(value = "Authorization", required = false) String token,
            HttpServletResponse response) {
        logout(token);
        // Clear httpOnly Cookie
        Cookie cookie = new Cookie("MANGO_TOKEN", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return R.ok();
    }

    /**
     * Validate token (HTTP endpoint)
     */
    @GetMapping("/validate")
    public R<Boolean> validateEndpoint(@RequestHeader(value = "Authorization", required = false) String token) {
        return R.ok(validateToken(token));
    }

    /**
     * Get client IP address, considering proxy headers
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP if multiple (client, proxy1, proxy2)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
