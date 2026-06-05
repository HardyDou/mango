package io.mango.auth.core.service.impl;

import io.mango.auth.api.AuthCode;
import io.mango.auth.api.command.WecomLoginCommand;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LoginTenantOptionsCommand;
import io.mango.auth.api.spi.LoginTenantProvider;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.api.vo.WecomLoginConfigVO;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.core.service.TokenRevocationService;
import io.mango.auth.core.service.WecomLoginClient;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.authorization.api.ITokenProvider;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
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

    private static final String DEFAULT_APP_CODE = "internal-admin";

    private final AuthUserProvider authUserProvider;
    private final IAuthorizationProvider authorizationProvider;
    private final ITokenProvider tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<LoginTenantProvider> loginTenantProvider;
    private final ObjectProvider<TokenRevocationService> tokenRevocationServiceProvider;
    private final IdentityUserApi identityUserApi;
    private final WecomLoginClient wecomLoginClient;
    private final NoticeApi noticeApi;

    @Value("${mango.security.jwt.access-token-validity:7200}")
    private long accessTokenValiditySeconds;

    @Value("${mango.security.jwt.refresh-token-validity:604800}")
    private long refreshTokenValiditySeconds;

    @Override
    public LoginVO login(LoginCommand command) {
        String username = command.getUsername();
        // 1. 校验账号。
        AuthUserInfo user = authUserProvider.getByUsernameForAuth(username, command.getRealm());
        Require.notNull(user, AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);

        // 2. 校验密码。
        Require.isTrue(passwordEncoder.matches(command.getPassword(), user.getPassword()),
                AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);

        // 3. 校验用户状态。
        Require.isTrue(user.getStatus() == 1, AuthCode.ACCOUNT_DISABLED);

        // 4. 生成令牌。
        IdentityContext identityContext = resolveIdentityContext(user, command);
        Map<String, Object> claims = identityContext.toClaims(user.getUsername());
        String accessToken = tokenService.generateAccessToken(user.getUserId(), user.getUsername(), claims);
        String refreshToken = tokenService.generateRefreshToken(user.getUserId(), user.getUsername(), claims);

        // 5. 构造响应。
        LoginVO response = buildLoginVO(user, identityContext, accessToken, refreshToken);

        // 6. 加载角色和权限。
        loadUserRolesAndPermissions(user.getUserId(), identityContext, response);

        log.info("User logged in successfully: {}", username);
        return response;
    }

    @Override
    public LoginVO loginByWecom(WecomLoginCommand command) {
        String tenantId = normalize(command.getTenantId());
        if (tenantId == null) {
            throw new BizException(AuthCode.INSTITUTION_REQUIRED.getCode(), "企业微信登录前请先选择机构");
        }
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.update(current -> current.withTenantId(tenantId));
            NoticeWecomLoginConfigVO loginConfig = resolveWecomLoginConfig(command.getChannelConfigId());
            String wecomUserId = wecomLoginClient.getUserId(loginConfig.getCorpId(), loginConfig.getSecret(), command.getCode());
            ExternalIdentityQuery query = new ExternalIdentityQuery();
            query.setProvider("WECOM");
            query.setCorpId(loginConfig.getCorpId());
            query.setExternalUserId(wecomUserId);
            var bindingResponse = identityUserApi.findExternalIdentity(query);
            ExternalIdentityBindingVO binding = bindingResponse == null || !bindingResponse.isSuccess()
                    ? null : bindingResponse.getData();
            if (binding == null || binding.getUserId() == null) {
                throw new BizException(1404, "当前企业微信账号尚未绑定 Mango 用户，请联系管理员绑定后再登录");
            }
            AuthUserInfo user = authUserProvider.getByIdForAuth(binding.getUserId());
            Require.notNull(user, AuthCode.CURRENT_USER_NOT_FOUND);
            Require.isTrue(user.getStatus() == 1, AuthCode.ACCOUNT_DISABLED);

            LoginCommand loginContext = new LoginCommand();
            loginContext.setTenantId(tenantId);
            loginContext.setTenantCode(command.getTenantCode());
            loginContext.setRealm(user.getRealm());
            loginContext.setActorType(user.getActorType());
            loginContext.setPartyType(user.getPartyType());
            loginContext.setPartyId(user.getPartyId());
            loginContext.setAppCode(firstText(command.getAppCode(), DEFAULT_APP_CODE));
            IdentityContext identityContext = resolveIdentityContext(user, loginContext);
            Map<String, Object> claims = identityContext.toClaims(user.getUsername());
            String accessToken = tokenService.generateAccessToken(user.getUserId(), user.getUsername(), claims);
            String refreshToken = tokenService.generateRefreshToken(user.getUserId(), user.getUsername(), claims);
            LoginVO response = buildLoginVO(user, identityContext, accessToken, refreshToken);
            loadUserRolesAndPermissions(user.getUserId(), identityContext, response);
            log.info("User logged in by WeCom successfully: userId={}, wecomUserId={}", user.getUserId(), wecomUserId);
            return response;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public WecomLoginConfigVO getWecomLoginConfig(String tenantId) {
        String normalizedTenantId = normalize(tenantId);
        if (normalizedTenantId == null) {
            throw new BizException(AuthCode.INSTITUTION_REQUIRED.getCode(), "请先选择机构");
        }
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.update(current -> current.withTenantId(normalizedTenantId));
            NoticeWecomLoginConfigVO noticeConfig = resolveWecomLoginConfig(null);
            WecomLoginConfigVO config = new WecomLoginConfigVO();
            config.setChannelConfigId(noticeConfig.getChannelConfigId());
            config.setCorpId(noticeConfig.getCorpId());
            config.setAgentId(noticeConfig.getAgentId());
            config.setRedirectUri(noticeConfig.getRedirectUri());
            return config;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public List<LoginTenantVO> listLoginTenants(LoginTenantOptionsCommand command) {
        AuthUserInfo user = authUserProvider.getByUsernameForAuth(command.getUsername(), command.getRealm());
        Require.notNull(user, AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        Require.isTrue(passwordEncoder.matches(command.getPassword(), user.getPassword()),
                AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        Require.isTrue(user.getStatus() == 1, AuthCode.ACCOUNT_DISABLED);

        LoginTenantProvider provider = loginTenantProvider.getIfAvailable();
        Require.notNull(provider, AuthCode.INSTITUTION_PROVIDER_UNAVAILABLE);
        List<LoginTenantVO> tenants = provider.listEnabledByUser(user.getUserId());
        Require.notEmpty(tenants, AuthCode.LOGIN_INSTITUTION_EMPTY);
        return tenants;
    }

    private NoticeWecomLoginConfigVO resolveWecomLoginConfig(Long channelConfigId) {
        R<NoticeWecomLoginConfigVO> response = noticeApi.getWecomLoginConfig(channelConfigId);
        NoticeWecomLoginConfigVO config = response == null || !response.isSuccess() ? null : response.getData();
        if (config == null) {
            String message = response == null ? null : response.getMsg();
            throw new BizException(1501, firstText(message, "企业微信扫码登录配置不存在或未启用"));
        }
        return config;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        String oldRefreshToken = refreshToken;
        if (oldRefreshToken != null && oldRefreshToken.startsWith("Bearer ")) {
            oldRefreshToken = oldRefreshToken.substring(7);
        }
        if (isRevoked(oldRefreshToken)) {
            log.warn("Refresh token has been revoked");
            return Require.fail(AuthCode.REFRESH_TOKEN_INVALID);
        }

        // 1. 校验并刷新令牌。
        ITokenProvider.TokenPair tokenPair = tokenService.refresh(oldRefreshToken);
        Require.notNull(tokenPair, AuthCode.REFRESH_TOKEN_INVALID);

        // 2. 从旧刷新令牌中读取用户 ID，此时旧令牌仍处于有效状态。
        Long userId = tokenService.getUserId(oldRefreshToken);
        Require.notNull(userId, AuthCode.REFRESH_TOKEN_INVALID);

        // 3. 加载用户。
        AuthUserInfo user = authUserProvider.getByIdForAuth(userId);
        Require.notNull(user, AuthCode.REFRESH_TOKEN_INVALID);
        Require.isTrue(user.getStatus() == 1, AuthCode.ACCOUNT_DISABLED);

        IdentityContext identityContext = resolveIdentityContext(user, oldRefreshToken);
        // 4. 构造响应。
        LoginVO response = buildLoginVO(user, identityContext, tokenPair.accessToken(), tokenPair.refreshToken());

        // 5. 加载角色和权限。
        loadUserRolesAndPermissions(user.getUserId(), identityContext, response);
        revoke(oldRefreshToken, refreshTokenValiditySeconds);

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
    private void loadUserRolesAndPermissions(Long userId, IdentityContext identityContext, LoginVO response) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.update(current -> current.withSecurity(
                    userId,
                    identityContext.memberId(),
                    identityContext.tenantId(),
                    response.getUsername(),
                    identityContext.realm(),
                    identityContext.actorType(),
                    identityContext.partyType(),
                    identityContext.partyId(),
                    identityContext.appCode()));
            var query = AuthorizationQuery.member(identityContext.memberId())
                    .withTenantId(identityContext.tenantId())
                    .withSystemCode(identityContext.appCode())
                    .withRealm(identityContext.realm())
                    .withActorType(identityContext.actorType())
                    .withParty(identityContext.partyType(), identityContext.partyId());
            var snapshot = authorizationProvider.load(query);
            response.setRoles(snapshot.roleCodes().stream().toList());
            response.setPermissions(snapshot.permissionCodes().stream().toList());
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private LoginVO buildLoginVO(AuthUserInfo user, IdentityContext identityContext,
                                 String accessToken, String refreshToken) {
        LoginVO response = new LoginVO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(accessTokenValiditySeconds);
        response.setTokenType("Bearer");
        response.setUserId(user.getUserId());
        response.setMemberId(identityContext.memberId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRealm(identityContext.realm());
        response.setActorType(identityContext.actorType());
        response.setPartyType(identityContext.partyType());
        response.setPartyId(identityContext.partyId());
        response.setTenantId(identityContext.tenantId());
        response.setTenantCode(identityContext.tenantCode());
        response.setTenantName(identityContext.tenantName());
        response.setAppCode(identityContext.appCode());
        return response;
    }

    private IdentityContext resolveIdentityContext(AuthUserInfo user, LoginCommand command) {
        LoginTenantVO tenant = resolveTenant(user.getUserId(), command.getTenantId(), command.getTenantCode());
        String partyType = firstText(command.getPartyType(), user.getPartyType());
        Long partyId = resolvePartyId(command.getPartyId(), user.getPartyId(), partyType, tenant.getTenantId());
        return new IdentityContext(
                firstText(command.getRealm(), user.getRealm()),
                firstText(command.getActorType(), user.getActorType()),
                partyType,
                partyId,
                tenant.getMemberId(),
                tenant.getTenantId(),
                tenant.getTenantCode(),
                tenant.getTenantName(),
                firstText(command.getAppCode(), DEFAULT_APP_CODE));
    }

    private IdentityContext resolveIdentityContext(AuthUserInfo user, String refreshToken) {
        Long partyId = resolveLong(tokenService.getClaim(refreshToken, "partyId"), user.getPartyId());
        Long memberId = resolveLong(tokenService.getClaim(refreshToken, "memberId"), null);
        String tenantId = normalize(tokenService.getClaim(refreshToken, "tenantId"));
        Require.notBlank(tenantId, AuthCode.REFRESH_TOKEN_INSTITUTION_CONTEXT_MISSING);
        LoginTenantVO tenant = resolveTenant(user.getUserId(), tenantId, tokenService.getClaim(refreshToken, "tenantCode"));
        Require.isTrue(memberId == null || memberId.equals(tenant.getMemberId()),
                AuthCode.REFRESH_TOKEN_MEMBER_CONTEXT_MISMATCH);
        return new IdentityContext(
                firstText(tokenService.getClaim(refreshToken, "realm"), user.getRealm()),
                firstText(tokenService.getClaim(refreshToken, "actorType"), user.getActorType()),
                firstText(tokenService.getClaim(refreshToken, "partyType"), user.getPartyType()),
                partyId,
                tenant.getMemberId(),
                tenant.getTenantId(),
                tenant.getTenantCode(),
                tenant.getTenantName(),
                firstText(tokenService.getClaim(refreshToken, "appCode"), DEFAULT_APP_CODE));
    }

    private LoginTenantVO resolveTenant(Long userId, @Nullable String tenantId, @Nullable String tenantCode) {
        String resolvedTenantId = normalize(tenantId);
        String resolvedTenantCode = normalize(tenantCode);
        Require.isFalse(resolvedTenantId == null && resolvedTenantCode == null, AuthCode.INSTITUTION_REQUIRED);
        LoginTenantProvider provider = loginTenantProvider.getIfAvailable();
        Require.notNull(provider, AuthCode.INSTITUTION_PROVIDER_UNAVAILABLE);
        LoginTenantVO tenant = resolvedTenantId != null
                ? provider.getEnabledByUserAndTenantId(userId, resolvedTenantId)
                : provider.getEnabledByUserAndTenantCode(userId, resolvedTenantCode);
        Require.notNull(tenant, AuthCode.INSTITUTION_ACCESS_DENIED);
        Require.notNull(tenant.getMemberId(), AuthCode.INSTITUTION_MEMBER_REQUIRED);
        return tenant;
    }

    private Long resolvePartyId(Long commandPartyId, Long userPartyId, String partyType, String tenantId) {
        if (commandPartyId != null) {
            return commandPartyId;
        }
        if ("INTERNAL_ORG".equals(partyType)) {
            Long parsedTenantId = resolveLong(tenantId, null);
            if (parsedTenantId != null) {
                return parsedTenantId;
            }
        }
        return userPartyId;
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

    private record IdentityContext(String realm,
                                   String actorType,
                                   String partyType,
                                   Long partyId,
                                   Long memberId,
                                   String tenantId,
                                   String tenantCode,
                                   String tenantName,
                                   String appCode) {
        Map<String, Object> toClaims(String username) {
            java.util.LinkedHashMap<String, Object> claims = new java.util.LinkedHashMap<>();
            claims.put("username", username);
            putIfPresent(claims, "realm", realm);
            putIfPresent(claims, "actorType", actorType);
            putIfPresent(claims, "partyType", partyType);
            putIfPresent(claims, "partyId", partyId);
            putIfPresent(claims, "memberId", memberId);
            putIfPresent(claims, "tenantId", tenantId);
            putIfPresent(claims, "tenantCode", tenantCode);
            putIfPresent(claims, "tenantName", tenantName);
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
