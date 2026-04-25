package io.mango.auth.core.service.impl;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.auth.api.vo.LoginResponse;
import io.mango.auth.core.service.IAuthService;
import io.mango.identity.api.IAuthUserProvider;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.infra.security.api.ITokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Authentication service implementation.
 * Delegates JWT operations to {@link ITokenService}.
 * Uses {@link IAuthorizationProvider} to load login-time authorization snapshot.
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IAuthUserProvider authUserProvider;
    private final IAuthorizationProvider authorizationProvider;
    private final ITokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Value("${mango.security.jwt.access-token-validity:7200}")
    private long accessTokenValiditySeconds;

    @Override
    public LoginResponse login(String username, String password) {
        // 1. Validate credentials
        AuthUserInfo user = authUserProvider.getByUsernameForAuth(username);
        if (user == null) {
            log.warn("Login failed: user not found - {}", username);
            return null;
        }

        // 2. Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed: invalid password for user - {}", username);
            return null;
        }

        // 3. Check user status
        if (user.getStatus() != 1) {
            log.warn("Login failed: user is disabled - {}", username);
            return null;
        }

        // 4. Generate tokens
        String accessToken = tokenService.generateAccessToken(
                user.getUserId(), user.getUsername(), Map.of("username", user.getUsername()));
        String refreshToken = tokenService.generateRefreshToken(user.getUserId(), user.getUsername());

        // 5. Build response
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(accessTokenValiditySeconds);
        response.setTokenType("Bearer");
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());

        // 6. Load roles and permissions
        loadUserRolesAndPermissions(user.getUserId(), response);

        log.info("User logged in successfully: {}", username);
        return response;
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // Strip "Bearer " prefix if present
        if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        // 1. Validate and refresh
        ITokenService.TokenPair tokenPair = tokenService.refresh(refreshToken);
        if (tokenPair == null) {
            log.warn("Refresh token is invalid or expired");
            return null;
        }

        // 2. Get userId from the old refresh token (still valid at this point)
        Long userId = tokenService.getUserId(refreshToken);
        if (userId == null) {
            return null;
        }

        // 3. Load user
        AuthUserInfo user = authUserProvider.getByIdForAuth(userId);
        if (user == null || user.getStatus() != 1) {
            log.warn("User not found or disabled during refresh: {}", userId);
            return null;
        }

        // 4. Build response
        LoginResponse response = new LoginResponse();
        response.setAccessToken(tokenPair.accessToken());
        response.setRefreshToken(tokenPair.refreshToken());
        response.setExpiresIn(accessTokenValiditySeconds);
        response.setTokenType("Bearer");
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());

        // 5. Load roles and permissions
        loadUserRolesAndPermissions(user.getUserId(), response);

        return response;
    }


    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = tokenService.getUserId(token);
        log.info("User logged out: userId={}", userId);
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return tokenService.validateToken(token);
    }

    public Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // Validate token before extracting userId to prevent tampered/expired token exploitation
        if (!tokenService.validateToken(token)) {
            return null;
        }
        return tokenService.getUserId(token);
    }

    /**
     * DRY extraction: loads user roles and permissions into LoginResponse.
     * Used by both login() and refreshToken().
     */
    private void loadUserRolesAndPermissions(Long userId, LoginResponse response) {
        var snapshot = authorizationProvider.load(AuthorizationQuery.user(userId));
        response.setRoles(snapshot.roleCodes().stream().toList());
        response.setPermissions(snapshot.permissionCodes().stream().toList());
    }
}
