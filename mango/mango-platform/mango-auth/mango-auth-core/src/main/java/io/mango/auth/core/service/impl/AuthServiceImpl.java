package io.mango.auth.core.service.impl;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.core.service.TokenRevocationService;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.infra.security.api.ITokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 认证服务实现。
 * JWT 操作委托给 {@link ITokenProvider}。
 * 登录时通过 {@link IAuthorizationProvider} 加载授权快照。
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final AuthUserProvider authUserProvider;
    private final IAuthorizationProvider authorizationProvider;
    private final ITokenProvider tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<TokenRevocationService> tokenRevocationServiceProvider;

    @Value("${mango.security.jwt.access-token-validity:7200}")
    private long accessTokenValiditySeconds;

    @Value("${mango.security.jwt.refresh-token-validity:604800}")
    private long refreshTokenValiditySeconds;

    @Override
    public LoginVO login(LoginCommand command) {
        String username = command.getUsername();
        // 1. 校验账号。
        AuthUserInfo user = authUserProvider.getByUsernameForAuth(username, command.getRealm());
        if (user == null) {
            log.warn("Login failed: user not found - {}", username);
            return null;
        }

        // 2. 校验密码。
        if (!passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for user - {}", username);
            return null;
        }

        // 3. 校验用户状态。
        if (user.getStatus() != 1) {
            log.warn("Login failed: user is disabled - {}", username);
            return null;
        }

        // 4. 生成令牌。
        IdentityContext identityContext = resolveIdentityContext(user, command);
        Map<String, Object> claims = identityContext.toClaims(user.getUsername());
        String accessToken = tokenService.generateAccessToken(user.getUserId(), user.getUsername(), claims);
        String refreshToken = tokenService.generateRefreshToken(user.getUserId(), user.getUsername(), claims);

        // 5. 构造响应。
        LoginVO response = buildLoginVO(user, identityContext, accessToken, refreshToken);

        // 6. 加载角色和权限。
        loadUserRolesAndPermissions(user.getUserId(), identityContext.appCode(), response);

        log.info("User logged in successfully: {}", username);
        return response;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 移除可能存在的 Bearer 前缀。
        if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        if (isRevoked(refreshToken)) {
            log.warn("Refresh token has been revoked");
            return null;
        }

        // 1. 校验并刷新令牌。
        ITokenProvider.TokenPair tokenPair = tokenService.refresh(refreshToken);
        if (tokenPair == null) {
            log.warn("Refresh token is invalid or expired");
            return null;
        }

        // 2. 从旧刷新令牌中读取用户 ID，此时旧令牌仍处于有效状态。
        Long userId = tokenService.getUserId(refreshToken);
        if (userId == null) {
            return null;
        }

        // 3. 加载用户。
        AuthUserInfo user = authUserProvider.getByIdForAuth(userId);
        if (user == null || user.getStatus() != 1) {
            log.warn("User not found or disabled during refresh: {}", userId);
            return null;
        }

        IdentityContext identityContext = resolveIdentityContext(user, refreshToken);
        // 4. 构造响应。
        LoginVO response = buildLoginVO(user, identityContext, tokenPair.accessToken(), tokenPair.refreshToken());

        // 5. 加载角色和权限。
        loadUserRolesAndPermissions(user.getUserId(), identityContext.appCode(), response);
        revoke(refreshToken, refreshTokenValiditySeconds);

        return response;
    }


    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = tokenService.getUserId(token);
        revoke(token, Math.max(accessTokenValiditySeconds, refreshTokenValiditySeconds));
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
        return tokenService.validateToken(token) && !isRevoked(token);
    }

    public Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // 提取用户 ID 前先校验令牌，避免篡改或过期令牌被利用。
        if (!tokenService.validateToken(token) || isRevoked(token)) {
            return null;
        }
        return tokenService.getUserId(token);
    }

    /**
     * 加载用户角色和权限到登录响应。
     * 登录和刷新令牌流程复用该逻辑。
     */
    private void loadUserRolesAndPermissions(Long userId, String appCode, LoginVO response) {
        var snapshot = authorizationProvider.load(AuthorizationQuery.user(userId).withSystemCode(appCode));
        response.setRoles(snapshot.roleCodes().stream().toList());
        response.setPermissions(snapshot.permissionCodes().stream().toList());
    }

    private LoginVO buildLoginVO(AuthUserInfo user, IdentityContext identityContext,
                                 String accessToken, String refreshToken) {
        LoginVO response = new LoginVO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(accessTokenValiditySeconds);
        response.setTokenType("Bearer");
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRealm(identityContext.realm());
        response.setActorType(identityContext.actorType());
        response.setPartyType(identityContext.partyType());
        response.setPartyId(identityContext.partyId());
        response.setAppCode(identityContext.appCode());
        return response;
    }

    private IdentityContext resolveIdentityContext(AuthUserInfo user, LoginCommand command) {
        return new IdentityContext(
                firstText(command.getRealm(), user.getRealm()),
                firstText(command.getActorType(), user.getActorType()),
                firstText(command.getPartyType(), user.getPartyType()),
                command.getPartyId() != null ? command.getPartyId() : user.getPartyId(),
                normalize(command.getAppCode()));
    }

    private IdentityContext resolveIdentityContext(AuthUserInfo user, String refreshToken) {
        Long partyId = resolveLong(tokenService.getClaim(refreshToken, "partyId"), user.getPartyId());
        return new IdentityContext(
                firstText(tokenService.getClaim(refreshToken, "realm"), user.getRealm()),
                firstText(tokenService.getClaim(refreshToken, "actorType"), user.getActorType()),
                firstText(tokenService.getClaim(refreshToken, "partyType"), user.getPartyType()),
                partyId,
                normalize(tokenService.getClaim(refreshToken, "appCode")));
    }

    private String firstText(String preferred, String fallback) {
        String value = normalize(preferred);
        return value != null ? value : normalize(fallback);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long resolveLong(String value, Long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private record IdentityContext(String realm, String actorType, String partyType, Long partyId, String appCode) {
        Map<String, Object> toClaims(String username) {
            java.util.LinkedHashMap<String, Object> claims = new java.util.LinkedHashMap<>();
            claims.put("username", username);
            putIfPresent(claims, "realm", realm);
            putIfPresent(claims, "actorType", actorType);
            putIfPresent(claims, "partyType", partyType);
            putIfPresent(claims, "partyId", partyId);
            putIfPresent(claims, "appCode", appCode);
            return claims;
        }

        private void putIfPresent(Map<String, Object> claims, String key, Object value) {
            if (value != null) {
                claims.put(key, value);
            }
        }
    }

    private boolean isRevoked(String token) {
        TokenRevocationService service = tokenRevocationServiceProvider.getIfAvailable();
        return service != null && service.isRevoked(token);
    }

    private void revoke(String token, long ttlSeconds) {
        TokenRevocationService service = tokenRevocationServiceProvider.getIfAvailable();
        if (service != null) {
            service.revoke(token, ttlSeconds);
        }
    }
}
