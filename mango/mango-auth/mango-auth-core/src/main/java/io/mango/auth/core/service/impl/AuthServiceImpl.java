package io.mango.auth.core.service.impl;

import io.mango.auth.api.vo.LoginRequest;
import io.mango.auth.api.vo.LoginResponse;
import io.mango.auth.api.vo.SysRoleVO;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.core.service.ISysRoleService;
import io.mango.infra.security.api.ITokenService;
import io.mango.permission.api.po.SysUser;
import io.mango.permission.api.SysMenuApi;
import io.mango.permission.api.SysUserApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication service implementation.
 * Delegates JWT operations to {@link ITokenService}.
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final SysUserApi sysUserApi;
    private final ISysRoleService sysRoleService;
    private final SysMenuApi sysMenuApi;
    private final ITokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Value("${mango.security.jwt.access-token-validity:7200}")
    private long accessTokenValiditySeconds;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 1. Validate credentials
        SysUser user = sysUserApi.getByUsernameForAuth(loginRequest.getUsername());
        if (user == null) {
            log.warn("Login failed: user not found - {}", loginRequest.getUsername());
            return null;
        }

        // 2. Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for user - {}", loginRequest.getUsername());
            return null;
        }

        // 3. Check user status
        if (user.getStatus() != 1) {
            log.warn("Login failed: user is disabled - {}", loginRequest.getUsername());
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

        log.info("User logged in successfully: {}", loginRequest.getUsername());
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
        SysUser user = sysUserApi.getByIdForAuth(userId);
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

    @Override
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
     * TODO: Replace sysRoleService/sysMenuApi with IPermissionChecker in Sprint-07 A6.
     */
    private void loadUserRolesAndPermissions(Long userId, LoginResponse response) {
        List<SysRoleVO> userRoles = sysRoleService.getUserRoles(userId);
        response.setRoles(userRoles != null && !userRoles.isEmpty()
                ? userRoles.stream().map(SysRoleVO::getRoleCode).collect(Collectors.toList())
                : List.of());

        Set<String> userPermissions = sysMenuApi.getUserPermissions(userId);
        response.setPermissions(userPermissions != null ? List.copyOf(userPermissions) : List.of());
    }
}
