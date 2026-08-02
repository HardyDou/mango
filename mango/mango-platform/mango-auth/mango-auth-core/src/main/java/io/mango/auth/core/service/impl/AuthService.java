package io.mango.auth.core.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.command.ChangeRequiredPasswordCommand;
import io.mango.auth.api.command.SendAuthCaptchaCommand;
import io.mango.auth.api.command.WecomLoginCommand;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LoginTenantOptionsCommand;
import io.mango.auth.api.spi.LoginTenantProvider;
import io.mango.auth.api.vo.ButtonDisplayRuleVO;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.api.vo.WecomLoginConfigVO;
import io.mango.common.result.CommonCode;
import io.mango.common.result.Require;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.core.service.IAuthProviderConfigService;
import io.mango.auth.core.store.TokenRevocationStore;
import io.mango.auth.core.store.PasswordResetTicketStore;
import io.mango.auth.core.service.WecomLoginClient;
import io.mango.auth.core.service.ExternalAccountLoginService;
import io.mango.identity.api.AuthIdentitySecurityProvider;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.vo.TokenPairVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.NoticeJsonRequest;
import io.mango.notice.api.command.NoticeSendEventCommand;
import io.mango.notice.api.command.NoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeSiteMessageSubjectCommand;
import io.mango.notice.api.command.NoticeSiteMessageTargetCommand;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.auth.core.support.AuthApiResponseAdapter;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.infra.iplocation.api.IpLocation;
import io.mango.infra.iplocation.api.IpLocationResolver;
import io.mango.system.api.command.RecordLoginLogCommand;
import io.mango.system.api.spi.LoginLogRecorder;
import io.mango.org.api.OrgReferenceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification = "Dependencies are shared Spring beans and Require.notNull throws before dereference")
public class AuthService implements IAuthService, ExternalAccountLoginService {

    private static final String DEFAULT_APP_CODE = "internal-admin";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int LOGIN_LOG_TEXT_LIMIT = 100;

    private final AuthUserProvider authUserProvider;
    private final IAuthorizationProvider authorizationProvider;
    private final ITokenProvider tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthIdentitySecurityProvider authIdentitySecurityProvider;
    private final PasswordResetTicketStore passwordResetTicketStore;
    private final LoginAttemptTracker loginAttemptTracker;
    private final ObjectProvider<LoginTenantProvider> loginTenantProvider;
    private final ObjectProvider<TenantMemberProvider> tenantMemberProvider;
    private final ObjectProvider<OrgReferenceProvider> orgReferenceProvider;
    private final ObjectProvider<TokenRevocationStore> tokenRevocationStoreProvider;
    private final IdentityUserApi identityUserApi;
    private final WecomLoginClient wecomLoginClient;
    private final IAuthProviderConfigService authProviderConfigService;
    private final NoticeApi noticeApi;
    private final ObjectProvider<CaptchaApi> captchaApiProvider;
    private final ObjectProvider<LoginLogRecorder> loginLogRecorderProvider;
    private final ObjectProvider<IpLocationResolver> ipLocationResolverProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${mango.security.jwt.access-token-validity:7200}")
    private long accessTokenValiditySeconds;

    @Value("${mango.security.jwt.refresh-token-validity:604800}")
    private long refreshTokenValiditySeconds;

    @Override
    public LoginVO login(LoginCommand command) {
        Require.notNull(command, AuthCode.AUTH_REQUEST_INVALID);
        try {
            LoginVO response = doLogin(command);
            if (!Boolean.TRUE.equals(response.getPasswordResetRequired())) {
                publishLoginSuccessNotice(command, response);
            }
            recordLoginLog(command, response, true, null);
            return response;
        } catch (io.mango.common.exception.BizException exception) {
            if (exception.getCode() == AuthCode.LOGIN_ATTEMPT_LOCKED.getCode()) {
                publishLoginLockedNotice(command);
            }
            recordLoginLog(command, null, false, exception.getMessage());
            return AuthApiResponseAdapter.rethrow(exception);
        }
    }

    private LoginVO doLogin(LoginCommand command) {
        String username = command.getUsername();
        String loginAttemptKey = loginAttemptKey(command.getRealm(), username);
        Require.isFalse(loginAttemptTracker.isLockedOut(loginAttemptKey), AuthCode.LOGIN_ATTEMPT_LOCKED);

        // 1. 校验账号。
        AuthUserVO user = authUserProvider.getByUsernameForAuth(username, command.getRealm());
        if (user == null) {
            loginAttemptTracker.recordFailedAttempt(loginAttemptKey);
            return Require.fail(AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        }
        authIdentitySecurityProvider.assertLoginAllowed(user);

        // 2. 校验密码。
        if (!passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            authIdentitySecurityProvider.recordLoginFailure(user.getUserId());
            return Require.fail(AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        }

        // 3. 校验用户状态。
        Require.isTrue(user.getStatus() == 1, AuthCode.ACCOUNT_DISABLED);
        if (Boolean.TRUE.equals(user.getPasswordResetRequired())) {
            LoginVO response = buildPasswordResetRequiredLoginVO(user, command);
            log.info("User login requires password change: userId={}, username={}", user.getUserId(), username);
            return response;
        }
        loginAttemptTracker.clearAttempts(loginAttemptKey);
        authIdentitySecurityProvider.recordLoginSuccess(user.getUserId());

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
    public AuthUserVO verifyBindingAccount(BindingCredentials credentials) {
        Require.notNull(credentials, AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        String username = credentials.username();
        Require.notBlank(username, AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        AuthUserVO user = authUserProvider.getByUsernameForAuth(username, "INTERNAL");
        Require.notNull(user, AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        authIdentitySecurityProvider.assertLoginAllowed(user);
        Require.isTrue(passwordEncoder.matches(credentials.password(), user.getPassword()),
                AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        Require.isTrue(user.getStatus() == 1, AuthCode.ACCOUNT_DISABLED);
        Require.isFalse(Boolean.TRUE.equals(user.getPasswordResetRequired()),
                AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        resolveTenant(user.getUserId(), credentials.tenantId(), null);
        return user;
    }

    @Override
    public LoginVO loginExternalUser(ExternalLoginContext context) {
        Require.notNull(context, AuthCode.AUTH_REQUEST_INVALID);
        Long userId = context.userId();
        AuthUserVO user = authUserProvider.getByIdForAuth(userId);
        Require.notNull(user, AuthCode.CURRENT_USER_NOT_FOUND);
        authIdentitySecurityProvider.assertLoginAllowed(user);
        Require.isTrue(user.getStatus() == 1, AuthCode.ACCOUNT_DISABLED);
        Require.isFalse(Boolean.TRUE.equals(user.getPasswordResetRequired()),
                AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        LoginCommand loginCommand = new LoginCommand();
        loginCommand.setTenantId(context.tenantId());
        loginCommand.setAppCode(firstText(context.appCode(), DEFAULT_APP_CODE));
        loginCommand.setRealm(user.getRealm());
        loginCommand.setActorType(user.getActorType());
        loginCommand.setPartyType(user.getPartyType());
        loginCommand.setPartyId(user.getPartyId());
        IdentityContext identityContext = resolveIdentityContext(user, loginCommand);
        Map<String, Object> claims = identityContext.toClaims(user.getUsername());
        String accessToken = tokenService.generateAccessToken(user.getUserId(), user.getUsername(), claims);
        String refreshToken = tokenService.generateRefreshToken(user.getUserId(), user.getUsername(), claims);
        LoginVO response = buildLoginVO(user, identityContext, accessToken, refreshToken);
        loadUserRolesAndPermissions(user.getUserId(), identityContext, response);
        authIdentitySecurityProvider.recordLoginSuccess(user.getUserId());
        return response;
    }

    @Override
    public LoginVO changeRequiredPassword(ChangeRequiredPasswordCommand command) {
        try {
            PasswordResetTicketStore.TicketPayload ticket = passwordResetTicketStore.peek(command.getPasswordResetTicket());
            AuthUserVO user = authUserProvider.getByIdForAuth(ticket.userId());
            Require.notNull(user, AuthCode.CURRENT_USER_NOT_FOUND);
            io.mango.identity.api.command.ChangeRequiredPasswordCommand identityCommand =
                    new io.mango.identity.api.command.ChangeRequiredPasswordCommand();
            identityCommand.setUserId(ticket.userId());
            identityCommand.setNewPassword(command.getNewPassword());
            identityCommand.setConfirmPassword(command.getConfirmPassword());
            authIdentitySecurityProvider.changeRequiredPassword(identityCommand);
            passwordResetTicketStore.revoke(command.getPasswordResetTicket());
            AuthUserVO updatedUser = authUserProvider.getByIdForAuth(ticket.userId());
            LoginCommand loginContext = new LoginCommand();
            loginContext.setTenantId(ticket.tenantId());
            loginContext.setTenantCode(ticket.tenantCode());
            loginContext.setAppCode(ticket.appCode());
            loginContext.setRealm(firstText(ticket.realm(), updatedUser.getRealm()));
            loginContext.setActorType(firstText(ticket.actorType(), updatedUser.getActorType()));
            loginContext.setPartyType(firstText(ticket.partyType(), updatedUser.getPartyType()));
            loginContext.setPartyId(firstNonNull(ticket.partyId(), updatedUser.getPartyId()));
            IdentityContext identityContext = resolveIdentityContext(updatedUser, loginContext);
            Map<String, Object> claims = identityContext.toClaims(updatedUser.getUsername());
            String accessToken = tokenService.generateAccessToken(updatedUser.getUserId(), updatedUser.getUsername(), claims);
            String refreshToken = tokenService.generateRefreshToken(updatedUser.getUserId(), updatedUser.getUsername(), claims);
            authIdentitySecurityProvider.recordLoginSuccess(updatedUser.getUserId());
            LoginVO response = buildLoginVO(updatedUser, identityContext, accessToken, refreshToken);
            loadUserRolesAndPermissions(updatedUser.getUserId(), identityContext, response);
            return response;
        } catch (IllegalArgumentException exception) {
            return Require.fail(AuthCode.AUTH_REQUEST_INVALID, exception.getMessage());
        }
    }

    @Override
    public LoginVO loginByWecom(WecomLoginCommand command) {
        String tenantId = normalize(command.getTenantId());
        Require.notBlank(tenantId, AuthCode.INSTITUTION_REQUIRED, "企业微信登录前请先选择机构");
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.update(current -> current.withTenantId(tenantId));
            String appCode = firstText(command.getAppCode(), DEFAULT_APP_CODE);
            IAuthProviderConfigService.ResolvedProviderConfig loginConfig =
                    resolveWecomLoginConfig(tenantId, appCode, command.getChannelConfigId());
            String wecomUserId = wecomLoginClient.getUserId(
                    loginConfig.providerTenantId(), loginConfig.secret(), command.getCode());
            ExternalIdentityQuery query = new ExternalIdentityQuery();
            query.setAppCode(appCode);
            query.setProvider("WECOM");
            query.setCorpId(loginConfig.providerTenantId());
            query.setExternalUserId(wecomUserId);
            ExternalIdentityBindingVO binding = AuthApiResponseAdapter.nullableData(
                    identityUserApi.findExternalIdentity(query));
            Require.notNull(binding, AuthCode.WECOM_ACCOUNT_UNBOUND);
            Require.notNull(binding.getUserId(), AuthCode.WECOM_ACCOUNT_UNBOUND);
            AuthUserVO user = authUserProvider.getByIdForAuth(binding.getUserId());
            Require.notNull(user, AuthCode.CURRENT_USER_NOT_FOUND);
            authIdentitySecurityProvider.assertLoginAllowed(user);
            Require.isTrue(user.getStatus() == 1, AuthCode.ACCOUNT_DISABLED);
            Require.isFalse(Boolean.TRUE.equals(user.getPasswordResetRequired()), AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);

            LoginCommand loginContext = new LoginCommand();
            loginContext.setTenantId(tenantId);
            loginContext.setTenantCode(command.getTenantCode());
            loginContext.setRealm(user.getRealm());
            loginContext.setActorType(user.getActorType());
            loginContext.setPartyType(user.getPartyType());
            loginContext.setPartyId(user.getPartyId());
            loginContext.setAppCode(appCode);
            IdentityContext identityContext = resolveIdentityContext(user, loginContext);
            Map<String, Object> claims = identityContext.toClaims(user.getUsername());
            String accessToken = tokenService.generateAccessToken(user.getUserId(), user.getUsername(), claims);
            String refreshToken = tokenService.generateRefreshToken(user.getUserId(), user.getUsername(), claims);
            LoginVO response = buildLoginVO(user, identityContext, accessToken, refreshToken);
            loadUserRolesAndPermissions(user.getUserId(), identityContext, response);
            authIdentitySecurityProvider.recordLoginSuccess(user.getUserId());
            log.info("User logged in by WeCom successfully: userId={}", user.getUserId());
            return response;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public WecomLoginConfigVO getWecomLoginConfig(String tenantId) {
        String normalizedTenantId = normalize(tenantId);
        Require.notBlank(normalizedTenantId, AuthCode.INSTITUTION_REQUIRED, "请先选择机构");
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.update(current -> current.withTenantId(normalizedTenantId));
            IAuthProviderConfigService.ResolvedProviderConfig providerConfig =
                    resolveWecomLoginConfig(normalizedTenantId, DEFAULT_APP_CODE, null);
            WecomLoginConfigVO config = new WecomLoginConfigVO();
            config.setChannelConfigId(providerConfig.id());
            config.setCorpId(providerConfig.providerTenantId());
            config.setAgentId(providerConfig.agentId());
            config.setRedirectUri(providerConfig.redirectUris().getFirst());
            return config;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public List<LoginTenantVO> listLoginTenants(LoginTenantOptionsCommand command) {
        AuthUserVO user = authUserProvider.getByUsernameForAuth(command.getUsername(), command.getRealm());
        Require.notNull(user, AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID);
        authIdentitySecurityProvider.assertLoginAllowed(user);
        Require.isTrue(user.getStatus() == 1, AuthCode.ACCOUNT_DISABLED);

        LoginTenantProvider provider = loginTenantProvider.getIfAvailable();
        Require.notNull(provider, AuthCode.INSTITUTION_PROVIDER_UNAVAILABLE);
        List<LoginTenantVO> tenants = provider.listEnabledByUser(user.getUserId());
        Require.notEmpty(tenants, AuthCode.LOGIN_INSTITUTION_EMPTY);
        return tenants;
    }

    private IAuthProviderConfigService.ResolvedProviderConfig resolveWecomLoginConfig(
            String tenantId, String appCode, Long configId) {
        IAuthProviderConfigService.ResolvedProviderConfig config = authProviderConfigService.requireAvailable(
                new IAuthProviderConfigService.ProviderSelection(tenantId, appCode, ExternalAuthProvider.WECOM));
        Require.isTrue(configId == null || configId.equals(config.id()), AuthCode.WECOM_CONFIG_UNAVAILABLE);
        return config;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        String oldRefreshToken = refreshToken;
        if (oldRefreshToken != null && oldRefreshToken.startsWith(BEARER_PREFIX)) {
            oldRefreshToken = oldRefreshToken.substring(BEARER_PREFIX.length());
        }
        if (isRevoked(oldRefreshToken)) {
            log.warn("Refresh token has been revoked");
            return Require.fail(AuthCode.REFRESH_TOKEN_INVALID);
        }

        // 1. 校验并刷新令牌。
        TokenPairVO tokenPair = tokenService.refresh(oldRefreshToken);
        Require.notNull(tokenPair, AuthCode.REFRESH_TOKEN_INVALID);

        // 2. 从旧刷新令牌中读取用户 ID，此时旧令牌仍处于有效状态。
        Long userId = tokenService.getUserId(oldRefreshToken);
        Require.notNull(userId, AuthCode.REFRESH_TOKEN_INVALID);

        // 3. 加载用户。
        AuthUserVO user = authUserProvider.getByIdForAuth(userId);
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
        if (token != null && token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
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
        if (token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }
        return tokenService.validateToken(token) && !isRevoked(token);
    }

    @Override
    public LoginVO info(String authorization) {
        String token = stripBearer(authorization);
        Long userId = tokenService.getUserId(token);
        Require.notNull(userId, AuthCode.ACCESS_TOKEN_INVALID);
        IdentityUserInfoVO userInfo = AuthApiResponseAdapter.requireIdentityData(
                identityUserApi.getUserInfoById(userId));
        LoginVO response = new LoginVO();
        response.setUserId(userInfo.getUserId());
        response.setMemberId(resolveLong(tokenService.getClaim(token, "memberId"), null));
        response.setUsername(userInfo.getUsername());
        response.setNickname(userInfo.getNickname());
        response.setRealm(firstText(tokenService.getClaim(token, "realm"), userInfo.getRealm()));
        response.setActorType(firstText(tokenService.getClaim(token, "actorType"), userInfo.getActorType()));
        response.setPartyType(firstText(tokenService.getClaim(token, "partyType"), userInfo.getPartyType()));
        response.setPartyId(resolveLong(tokenService.getClaim(token, "partyId"), userInfo.getPartyId()));
        String appCode = tokenService.getClaim(token, "appCode");
        String tenantId = tokenService.getClaim(token, "tenantId");
        response.setTenantId(tenantId);
        response.setTenantCode(tokenService.getClaim(token, "tenantCode"));
        response.setTenantName(tokenService.getClaim(token, "tenantName"));
        response.setAppCode(appCode);
        populateOrganizationLabels(response);
        Require.notNull(response.getMemberId(), AuthCode.INSTITUTION_MEMBER_REQUIRED);
        var snapshot = authorizationProvider.load(AuthorizationQuery.member(response.getMemberId())
                .withTenantId(tenantId)
                .withSystemCode(appCode)
                .withRealm(response.getRealm())
                .withActorType(response.getActorType())
                .withParty(response.getPartyType(), response.getPartyId()));
        response.setRoles(snapshot.roleCodes().stream().toList());
        response.setPermissions(snapshot.permissionCodes().stream().toList());
        response.setButtonRules(snapshot.buttonRules().stream().map(this::toLoginButtonRule).toList());
        return response;
    }

    @Override
    public String sendCaptcha(SendAuthCaptchaCommand command) {
        CaptchaApi captchaApi = captchaApiProvider.getIfAvailable();
        Require.notNull(captchaApi, AuthCode.CAPTCHA_SERVICE_UNAVAILABLE);
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(CaptchaType.valueOf(command.getType().name()));
        request.setTarget(command.getTarget());
        request.setBusinessType(command.getBusinessType());
        request.setExpireSeconds(command.getExpireSeconds());
        return AuthApiResponseAdapter.requireCaptchaData(captchaApi.send(request));
    }

    public Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }
        // 提取用户 ID 前先校验令牌，避免篡改或过期令牌被利用。
        if (!tokenService.validateToken(token) || isRevoked(token)) {
            return null;
        }
        return tokenService.getUserId(token);
    }

    private String stripBearer(String token) {
        if (token != null && token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        return token;
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
            response.setButtonRules(snapshot.buttonRules().stream()
                    .map(this::toLoginButtonRule)
                    .toList());
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private ButtonDisplayRuleVO toLoginButtonRule(io.mango.authorization.api.vo.ButtonDisplayRuleVO source) {
        ButtonDisplayRuleVO target = new ButtonDisplayRuleVO();
        target.setCode(source.getCode());
        target.setButtonType(source.getButtonType());
        target.setDisplayRule(source.getDisplayRule());
        return target;
    }

    private void publishLoginSuccessNotice(LoginCommand command, LoginVO response) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("username", response.getUsername());
        params.put("clientIp", command.getClientIp());
        params.put("loginTime", LocalDateTime.now().toString());
        params.put("appCode", firstText(response.getAppCode(), command.getAppCode()));
        NoticeSiteMessageTargetCommand target = routeTarget("account:profile", params);
        NoticeSendEventCommand event = new NoticeSendEventCommand();
        event.setTenantId(response.getTenantId());
        event.setBizType("auth.login.success");
        event.setBizId(String.valueOf(response.getUserId()));
        event.setUserId(response.getUserId());
        event.setParams(NoticeJsonRequest.of(params));
        event.setMessageScene("auth.login.success");
        event.setMessageSubject(subject("AUTH_LOGIN", String.valueOf(response.getUserId()), response.getUsername()));
        event.setMessageTarget(target);
        event.setMessageData(NoticeJsonRequest.of(params));
        event.setMessageActions(List.of(routeAction("VIEW_PROFILE", "查看资料", target)));
        event.setPriority(NoticePriority.LOW);
        event.setIdempotentKey("auth.login.success:" + response.getUserId() + ":" + System.currentTimeMillis());
        eventPublisher.publishEvent(event);
    }

    private void publishLoginLockedNotice(LoginCommand command) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("username", command.getUsername());
        params.put("clientIp", command.getClientIp());
        params.put("loginTime", LocalDateTime.now().toString());
        NoticeSiteMessageTargetCommand target = routeTarget("system:user", params);
        NoticeSendEventCommand event = new NoticeSendEventCommand();
        event.setTenantId(firstText(command.getTenantId(), MangoContextHolder.tenantId()));
        event.setBizType("auth.login.locked");
        event.setBizId(command.getUsername());
        event.setParams(NoticeJsonRequest.of(params));
        event.setMessageScene("auth.login.locked");
        event.setMessageSubject(subject("AUTH_LOGIN_LOCK", command.getUsername(), command.getUsername()));
        event.setMessageTarget(target);
        event.setMessageData(NoticeJsonRequest.of(params));
        event.setMessageActions(List.of(routeAction("VIEW_USER", "查看账号", target)));
        event.setPriority(NoticePriority.HIGH);
        event.setIdempotentKey("auth.login.locked:" + command.getUsername() + ":" + command.getClientIp());
        eventPublisher.publishEvent(event);
    }

    private NoticeSiteMessageSubjectCommand subject(String type, String id, String name) {
        NoticeSiteMessageSubjectCommand subject = new NoticeSiteMessageSubjectCommand();
        subject.setSubjectType(type);
        subject.setSubjectId(id);
        subject.setSubjectName(name);
        return subject;
    }

    private NoticeSiteMessageTargetCommand routeTarget(String key, Map<String, Object> params) {
        NoticeSiteMessageTargetCommand target = new NoticeSiteMessageTargetCommand();
        target.setTargetType(NoticeSiteMessageTargetType.ROUTE);
        target.setTargetKey(key);
        target.setParams(NoticeJsonRequest.of(params));
        return target;
    }

    private NoticeSiteMessageActionCommand routeAction(
            String code, String label, NoticeSiteMessageTargetCommand target) {
        NoticeSiteMessageActionCommand action = new NoticeSiteMessageActionCommand();
        action.setActionCode(code);
        action.setActionLabel(label);
        action.setInteractionType(NoticeSiteMessageActionInteractionType.ROUTE);
        action.setTarget(target);
        return action;
    }

    private void recordLoginLog(
            LoginCommand command, LoginVO response, boolean success, String failureMessage) {
        LoginLogRecorder recorder = loginLogRecorderProvider.getIfAvailable();
        if (recorder == null) {
            return;
        }
        try {
            RecordLoginLogCommand loginLog = new RecordLoginLogCommand();
            String tenantId = command.getTenantId();
            Long userId = null;
            String loginType = "PASSWORD";
            if (response != null) {
                tenantId = response.getTenantId();
                userId = response.getUserId();
                loginType = response.getRealm();
            }
            loginLog.setTenantId(tenantId);
            loginLog.setUserId(userId);
            loginLog.setUsername(command.getUsername());
            loginLog.setLoginType(firstText(command.getRealm(), loginType));
            loginLog.setIp(command.getClientIp());
            loginLog.setLocation(resolveLocation(command.getClientIp()));
            loginLog.setBrowser(truncate(firstText(command.getUserAgent(), "未知"), LOGIN_LOG_TEXT_LIMIT));
            loginLog.setOs("未知");
            populateLoginResult(loginLog, success, failureMessage);
            loginLog.setLoginTime(LocalDateTime.now());
            recorder.record(loginLog);
        } catch (RuntimeException exception) {
            log.warn("Failed to record login log for {}", command.getUsername(), exception);
        }
    }

    private void populateLoginResult(RecordLoginLogCommand loginLog, boolean success, String failureMessage) {
        if (success) {
            loginLog.setStatus(1);
            loginLog.setMsg(CommonCode.SUCCESS.getMessage());
            return;
        }
        loginLog.setStatus(0);
        loginLog.setMsg(failureMessage);
    }

    private String resolveLocation(String clientIp) {
        IpLocationResolver resolver = ipLocationResolverProvider.getIfAvailable();
        if (resolver == null) {
            return "未知";
        }
        IpLocation location = resolver.resolve(clientIp);
        if (location == null) {
            return "未知";
        }
        return location.displayText();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private LoginVO buildLoginVO(AuthUserVO user, IdentityContext identityContext,
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
        response.setPasswordResetRequired(Boolean.FALSE);
        populateOrganizationLabels(response);
        return response;
    }

    private LoginVO buildPasswordResetRequiredLoginVO(AuthUserVO user, LoginCommand command) {
        LoginVO response = new LoginVO();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRealm(firstText(command.getRealm(), user.getRealm()));
        response.setActorType(firstText(command.getActorType(), user.getActorType()));
        response.setPartyType(firstText(command.getPartyType(), user.getPartyType()));
        response.setPartyId(firstNonNull(command.getPartyId(), user.getPartyId()));
        response.setAppCode(firstText(command.getAppCode(), DEFAULT_APP_CODE));
        response.setPasswordResetRequired(Boolean.TRUE);
        response.setLoginAction("CHANGE_PASSWORD");
        LoginTenantVO tenant = resolveTenant(user.getUserId(), command.getTenantId(), command.getTenantCode());
        response.setMemberId(tenant.getMemberId());
        response.setTenantId(tenant.getTenantId());
        response.setTenantCode(tenant.getTenantCode());
        response.setTenantName(tenant.getTenantName());
        populateOrganizationLabels(response);
        response.setPasswordResetTicket(passwordResetTicketStore.issue(new PasswordResetTicketStore.TicketPayload(
                user.getUserId(),
                tenant.getTenantId(),
                tenant.getTenantCode(),
                response.getAppCode(),
                response.getRealm(),
                response.getActorType(),
                response.getPartyType(),
                response.getPartyId())));
        return response;
    }

    private void populateOrganizationLabels(LoginVO response) {
        response.setCompanyName(response.getTenantName());
        if (response.getMemberId() == null) {
            return;
        }
        TenantMemberProvider memberProvider = tenantMemberProvider.getIfAvailable();
        OrgReferenceProvider organizationProvider = orgReferenceProvider.getIfAvailable();
        if (memberProvider == null || organizationProvider == null) {
            return;
        }
        TenantMemberVO member = memberProvider.getMember(response.getMemberId());
        if (member == null || member.getPrimaryOrgId() == null) {
            return;
        }
        Long tenantId = resolveLong(response.getTenantId(), member.getTenantId());
        response.setDepartmentName(organizationProvider.resolveOrgName(tenantId, member.getPrimaryOrgId()));
    }

    private IdentityContext resolveIdentityContext(AuthUserVO user, LoginCommand command) {
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

    private IdentityContext resolveIdentityContext(AuthUserVO user, String refreshToken) {
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
        LoginTenantVO tenant;
        if (resolvedTenantId != null) {
            tenant = provider.getEnabledByUserAndTenantId(userId, resolvedTenantId);
        } else {
            tenant = provider.getEnabledByUserAndTenantCode(userId, resolvedTenantCode);
        }
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
        if (value != null) {
            return value;
        }
        return normalize(fallback);
    }

    private String loginAttemptKey(String realm, String username) {
        return firstText(realm, "INTERNAL").toUpperCase() + ":" + firstText(username, "").toLowerCase();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        if (preferred != null) {
            return preferred;
        }
        return fallback;
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
        TokenRevocationStore store = tokenRevocationStoreProvider.getIfAvailable();
        return store != null && store.isRevoked(token);
    }

    private void revoke(String token, long ttlSeconds) {
        TokenRevocationStore store = tokenRevocationStoreProvider.getIfAvailable();
        if (store != null) {
            store.revoke(token, ttlSeconds);
        }
    }
}
