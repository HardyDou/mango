package io.mango.auth.core.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.auth.api.command.BindExistingAccountCommand;
import io.mango.auth.api.command.CompleteProviderAuthorizationCommand;
import io.mango.auth.api.command.StartProviderAuthorizationCommand;
import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.enums.ProviderAuthorizationIntent;
import io.mango.auth.api.enums.ProviderAuthorizationStatus;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.api.vo.ProviderAuthorizationResultVO;
import io.mango.auth.api.vo.ProviderAuthorizationVO;
import io.mango.auth.core.service.ExternalAccountLoginService;
import io.mango.auth.core.service.ExternalAuthProviderAdapter;
import io.mango.auth.core.service.IAuthProviderConfigService;
import io.mango.auth.core.service.IExternalAuthorizationService;
import io.mango.auth.core.store.ProviderAuthorizationStore;
import io.mango.auth.core.support.AuthApiResponseAdapter;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.vo.SecurityContextVO;
import io.mango.common.result.Require;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ExternalAuthorizationService implements IExternalAuthorizationService {

    private final IAuthProviderConfigService configService;
    private final ProviderAuthorizationStore authorizationStore;
    private final IdentityUserApi identityUserApi;
    private final ExternalAccountLoginService accountLoginService;
    private final ISecurityContextProvider securityContextProvider;
    private final Map<ExternalAuthProvider, ExternalAuthProviderAdapter> adapters;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "The service intentionally retains Spring-managed domain collaborators")
    public ExternalAuthorizationService(IAuthProviderConfigService configService,
                                        ProviderAuthorizationStore authorizationStore,
                                        IdentityUserApi identityUserApi,
                                        ExternalAccountLoginService accountLoginService,
                                        ISecurityContextProvider securityContextProvider,
                                        List<ExternalAuthProviderAdapter> adapters) {
        this.configService = configService;
        this.authorizationStore = authorizationStore;
        this.identityUserApi = identityUserApi;
        this.accountLoginService = accountLoginService;
        this.securityContextProvider = securityContextProvider;
        this.adapters = new EnumMap<>(ExternalAuthProvider.class);
        adapters.forEach(adapter -> this.adapters.put(adapter.provider(), adapter));
    }

    @Override
    public ProviderAuthorizationVO start(StartProviderAuthorizationCommand command) {
        Require.notNull(command, AuthCode.AUTH_REQUEST_INVALID);
        IAuthProviderConfigService.ResolvedProviderConfig config = configService.requireAvailable(
                new IAuthProviderConfigService.ProviderSelection(
                        command.getTenantId(), command.getAppCode(), command.getProvider()));
        String redirectUri = command.getRedirectUri().trim();
        Require.isTrue(config.redirectUris().contains(redirectUri), AuthCode.PROVIDER_REDIRECT_INVALID);
        Long userId = null;
        if (command.getIntent() == ProviderAuthorizationIntent.BIND_CURRENT) {
            SecurityContextVO securityContext = securityContextProvider.currentContext();
            userId = Require.nonNull(securityContext.userId(), AuthCode.ACCESS_TOKEN_INVALID,
                    "绑定第三方授权前请先登录");
            Require.isTrue(config.tenantId().equals(securityContext.tenantId())
                            && config.appCode().equals(securityContext.appCode()),
                    AuthCode.INSTITUTION_ACCESS_DENIED, "只能绑定当前登录租户和应用");
        }
        String state = authorizationStore.issueState(new ProviderAuthorizationStore.StatePayload(
                config.tenantId(), config.appCode(), config.provider(), command.getIntent(), redirectUri, userId));
        ExternalAuthProviderAdapter adapter = requireAdapter(config.provider());
        return new ProviderAuthorizationVO(adapter.buildAuthorizationUrl(config, redirectUri, state),
                authorizationStore.stateTtlSeconds());
    }

    @Override
    public ProviderAuthorizationResultVO complete(CompleteProviderAuthorizationCommand command) {
        Require.notNull(command, AuthCode.AUTH_REQUEST_INVALID);
        ProviderAuthorizationStore.StatePayload state = authorizationStore.consumeState(command.getState());
        IAuthProviderConfigService.ResolvedProviderConfig config = configService.requireAvailable(
                new IAuthProviderConfigService.ProviderSelection(
                        state.tenantId(), state.appCode(), state.provider()));
        ExternalAuthProviderAdapter.ExternalAuthIdentity externalIdentity = requireAdapter(state.provider())
                .exchange(config, command.getCode());
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.update(current -> current.withRequest(null, null, state.tenantId(), state.appCode(), null));
            ExternalIdentityBindingVO existing = findBinding(state, externalIdentity);
            if (state.intent() == ProviderAuthorizationIntent.BIND_CURRENT) {
                return completeCurrentBinding(state, externalIdentity, existing);
            }
            if (existing != null) {
                return loginResult(existing.getUserId(), state);
            }
            String ticket = authorizationStore.issueBinding(new ProviderAuthorizationStore.BindingPayload(
                    state.tenantId(), state.appCode(), state.provider(), externalIdentity.providerTenantId(),
                    externalIdentity.externalUserId(), externalIdentity.displayName()));
            ProviderAuthorizationResultVO result = new ProviderAuthorizationResultVO();
            result.setStatus(ProviderAuthorizationStatus.BIND_REQUIRED);
            result.setBindingTicket(ticket);
            result.setProviderDisplayName(externalIdentity.displayName());
            result.setExpiresInSeconds(authorizationStore.bindTicketTtlSeconds());
            return result;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    @Override
    public LoginVO bindExisting(BindExistingAccountCommand command) {
        Require.notNull(command, AuthCode.AUTH_REQUEST_INVALID);
        ProviderAuthorizationStore.BindingPayload ticket = authorizationStore.consumeBinding(
                command.getBindingTicket());
        AuthUserVO user = accountLoginService.verifyBindingAccount(new ExternalAccountLoginService.BindingCredentials(
                command.getUsername(), command.getPassword(), ticket.tenantId()));
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.update(current -> current.withRequest(null, null, ticket.tenantId(), ticket.appCode(), null));
            bind(user.getUserId(), ticket.appCode(), ticket.provider(), ticket.providerTenantId(),
                    ticket.externalUserId(), ticket.displayName());
            return accountLoginService.loginExternalUser(new ExternalAccountLoginService.ExternalLoginContext(
                    user.getUserId(), ticket.tenantId(), ticket.appCode()));
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private ProviderAuthorizationResultVO completeCurrentBinding(
            ProviderAuthorizationStore.StatePayload state,
            ExternalAuthProviderAdapter.ExternalAuthIdentity externalIdentity,
            ExternalIdentityBindingVO existing) {
        Long userId = Require.nonNull(state.userId(), AuthCode.ACCESS_TOKEN_INVALID);
        if (existing != null) {
            Require.isTrue(userId.equals(existing.getUserId()), AuthCode.EXTERNAL_BINDING_CONFLICT);
        } else {
            bind(userId, state.appCode(), state.provider(), externalIdentity.providerTenantId(),
                    externalIdentity.externalUserId(), externalIdentity.displayName());
        }
        ProviderAuthorizationResultVO result = new ProviderAuthorizationResultVO();
        result.setStatus(ProviderAuthorizationStatus.BIND_SUCCESS);
        result.setProviderDisplayName(externalIdentity.displayName());
        return result;
    }

    private ProviderAuthorizationResultVO loginResult(Long userId,
                                                      ProviderAuthorizationStore.StatePayload state) {
        ProviderAuthorizationResultVO result = new ProviderAuthorizationResultVO();
        result.setStatus(ProviderAuthorizationStatus.LOGIN_SUCCESS);
        result.setLogin(accountLoginService.loginExternalUser(new ExternalAccountLoginService.ExternalLoginContext(
                userId, state.tenantId(), state.appCode())));
        return result;
    }

    private ExternalIdentityBindingVO findBinding(ProviderAuthorizationStore.StatePayload state,
                                                   ExternalAuthProviderAdapter.ExternalAuthIdentity identity) {
        ExternalIdentityQuery query = new ExternalIdentityQuery();
        query.setAppCode(state.appCode());
        query.setProvider(state.provider().name());
        query.setCorpId(identity.providerTenantId());
        query.setExternalUserId(identity.externalUserId());
        return AuthApiResponseAdapter.nullableData(identityUserApi.findExternalIdentity(query));
    }

    private ExternalIdentityBindingVO bind(Long userId, String appCode, ExternalAuthProvider provider,
                                           String providerTenantId, String externalUserId, String displayName) {
        BindExternalIdentityCommand command = new BindExternalIdentityCommand();
        command.setUserId(userId);
        command.setAppCode(appCode);
        command.setProvider(provider.name());
        command.setCorpId(providerTenantId);
        command.setExternalUserId(externalUserId);
        command.setDisplayName(displayName);
        command.setBindSource("SELF");
        return AuthApiResponseAdapter.requireIdentityData(identityUserApi.bindExternalIdentity(command));
    }

    private ExternalAuthProviderAdapter requireAdapter(ExternalAuthProvider provider) {
        return Require.nonNull(adapters.get(provider), AuthCode.PROVIDER_CONFIG_UNAVAILABLE);
    }
}
