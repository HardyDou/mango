package io.mango.auth.core.service.impl;

import io.mango.auth.api.command.BindExistingAccountCommand;
import io.mango.auth.api.command.CompleteProviderAuthorizationCommand;
import io.mango.auth.api.command.StartProviderAuthorizationCommand;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.enums.ProviderAuthorizationIntent;
import io.mango.auth.api.enums.ProviderAuthorizationStatus;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.api.vo.ProviderAuthorizationResultVO;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.vo.SecurityContextVO;
import io.mango.auth.core.service.ExternalAccountLoginService;
import io.mango.auth.core.service.ExternalAuthProviderAdapter;
import io.mango.auth.core.service.IAuthProviderConfigService;
import io.mango.auth.core.store.ProviderAuthorizationStore;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalAuthorizationServiceTest {

    private IAuthProviderConfigService configService;
    private ProviderAuthorizationStore authorizationStore;
    private IdentityUserApi identityUserApi;
    private ExternalAccountLoginService accountLoginService;
    private ISecurityContextProvider securityContextProvider;
    private ExternalAuthProviderAdapter adapter;
    private ExternalAuthorizationService service;

    @BeforeEach
    void setUp() {
        configService = mock(IAuthProviderConfigService.class);
        authorizationStore = mock(ProviderAuthorizationStore.class);
        identityUserApi = mock(IdentityUserApi.class);
        accountLoginService = mock(ExternalAccountLoginService.class);
        securityContextProvider = mock(ISecurityContextProvider.class);
        adapter = mock(ExternalAuthProviderAdapter.class);
        when(adapter.provider()).thenReturn(ExternalAuthProvider.WECOM);
        service = new ExternalAuthorizationService(configService, authorizationStore, identityUserApi,
                accountLoginService, securityContextProvider, List.of(adapter));
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withRequest(null, null, "current-tenant", "current-app", null));
    }

    @Test
    void startsCurrentBindingFromCanonicalSecurityContext() {
        StartProviderAuthorizationCommand command = startCommand(ProviderAuthorizationIntent.BIND_CURRENT);
        when(configService.requireAvailable("tenant-a", "admin-app", ExternalAuthProvider.WECOM))
                .thenReturn(config());
        when(securityContextProvider.currentContext()).thenReturn(new SecurityContextVO(
                7L, null, "tenant-a", true, "alice", null, null, null, null, "admin-app"));
        when(authorizationStore.issueState(any())).thenReturn("state");
        when(authorizationStore.stateTtlSeconds()).thenReturn(300L);
        when(adapter.buildAuthorizationUrl(any(), eq("https://admin.example.com/provider-callback"), eq("state")))
                .thenReturn("https://provider.example.com/authorize");

        var result = service.start(command);

        ArgumentCaptor<ProviderAuthorizationStore.StatePayload> stateCaptor =
                ArgumentCaptor.forClass(ProviderAuthorizationStore.StatePayload.class);
        verify(authorizationStore).issueState(stateCaptor.capture());
        assertThat(stateCaptor.getValue().userId()).isEqualTo(7L);
        assertThat(stateCaptor.getValue().intent()).isEqualTo(ProviderAuthorizationIntent.BIND_CURRENT);
        assertThat(result.getAuthorizationUrl()).isEqualTo("https://provider.example.com/authorize");
        assertThat(result.getExpiresInSeconds()).isEqualTo(300L);
    }

    @Test
    void rejectsCurrentBindingOutsideAuthenticatedTenantAndApplication() {
        StartProviderAuthorizationCommand command = startCommand(ProviderAuthorizationIntent.BIND_CURRENT);
        when(configService.requireAvailable("tenant-a", "admin-app", ExternalAuthProvider.WECOM))
                .thenReturn(config());
        when(securityContextProvider.currentContext()).thenReturn(new SecurityContextVO(
                7L, null, "tenant-b", true, "alice", null, null, null, null, "other-app"));

        assertThatThrownBy(() -> service.start(command)).isInstanceOf(BizException.class);

        verify(authorizationStore, never()).issueState(any());
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void completesBoundExternalIdentityAsLogin() {
        ProviderAuthorizationStore.StatePayload state = loginState();
        ExternalIdentityBindingVO binding = binding(42L);
        LoginVO login = new LoginVO();
        login.setAccessToken("access-token");
        when(authorizationStore.consumeState("state")).thenReturn(state);
        when(configService.requireAvailable("tenant-a", "admin-app", ExternalAuthProvider.WECOM))
                .thenReturn(config());
        when(adapter.exchange(any(), eq("authorization-code")))
                .thenReturn(new ExternalAuthProviderAdapter.ExternalAuthIdentity("corp-a", "external-a", "Alice"));
        when(identityUserApi.findExternalIdentity(any())).thenReturn(R.ok(binding));
        when(accountLoginService.loginExternalUser(42L, "tenant-a", "admin-app")).thenReturn(login);

        ProviderAuthorizationResultVO result = service.complete(completeCommand());

        assertThat(result.getStatus()).isEqualTo(ProviderAuthorizationStatus.LOGIN_SUCCESS);
        assertThat(result.getLogin()).isSameAs(login);
        verify(identityUserApi, never()).bindExternalIdentity(any());
        verify(authorizationStore, never()).issueBinding(any());
    }

    @Test
    void requiresExistingAccountBindingForUnboundExternalIdentity() {
        when(authorizationStore.consumeState("state")).thenReturn(loginState());
        when(configService.requireAvailable("tenant-a", "admin-app", ExternalAuthProvider.WECOM))
                .thenReturn(config());
        when(adapter.exchange(any(), eq("authorization-code")))
                .thenReturn(new ExternalAuthProviderAdapter.ExternalAuthIdentity("corp-a", "external-a", "Alice"));
        when(identityUserApi.findExternalIdentity(any())).thenReturn(R.ok());
        when(authorizationStore.issueBinding(any())).thenReturn("binding-ticket");
        when(authorizationStore.bindTicketTtlSeconds()).thenReturn(600L);

        ProviderAuthorizationResultVO result = service.complete(completeCommand());

        assertThat(result.getStatus()).isEqualTo(ProviderAuthorizationStatus.BIND_REQUIRED);
        assertThat(result.getBindingTicket()).isEqualTo("binding-ticket");
        assertThat(result.getProviderDisplayName()).isEqualTo("Alice");
        assertThat(result.getExpiresInSeconds()).isEqualTo(600L);
        verify(accountLoginService, never()).loginExternalUser(any(), any(), any());
    }

    @Test
    void rejectsCurrentUserBindingWhenExternalIdentityBelongsToAnotherUser() {
        ProviderAuthorizationStore.StatePayload state = new ProviderAuthorizationStore.StatePayload(
                "tenant-a", "admin-app", ExternalAuthProvider.WECOM, ProviderAuthorizationIntent.BIND_CURRENT,
                "https://admin.example.com/provider-callback", 7L);
        when(authorizationStore.consumeState("state")).thenReturn(state);
        when(configService.requireAvailable("tenant-a", "admin-app", ExternalAuthProvider.WECOM))
                .thenReturn(config());
        when(adapter.exchange(any(), eq("authorization-code")))
                .thenReturn(new ExternalAuthProviderAdapter.ExternalAuthIdentity("corp-a", "external-a", "Alice"));
        when(identityUserApi.findExternalIdentity(any())).thenReturn(R.ok(binding(8L)));

        assertThatThrownBy(() -> service.complete(completeCommand())).isInstanceOf(BizException.class);

        verify(identityUserApi, never()).bindExternalIdentity(any());
    }

    @Test
    void bindsConsumedTicketToVerifiedExistingAccountAndLogsIn() {
        ProviderAuthorizationStore.BindingPayload ticket = new ProviderAuthorizationStore.BindingPayload(
                "tenant-a", "admin-app", ExternalAuthProvider.WECOM, "corp-a", "external-a", "Alice");
        AuthUserVO user = new AuthUserVO();
        user.setUserId(42L);
        LoginVO login = new LoginVO();
        login.setAccessToken("access-token");
        BindExistingAccountCommand command = new BindExistingAccountCommand();
        command.setBindingTicket("binding-ticket");
        command.setUsername("alice");
        command.setPassword("current-password");
        when(authorizationStore.consumeBinding("binding-ticket")).thenReturn(ticket);
        when(accountLoginService.verifyBindingAccount("alice", "current-password", "tenant-a")).thenReturn(user);
        when(identityUserApi.bindExternalIdentity(any())).thenReturn(R.ok(binding(42L)));
        when(accountLoginService.loginExternalUser(42L, "tenant-a", "admin-app")).thenReturn(login);

        assertThat(service.bindExisting(command)).isSameAs(login);

        ArgumentCaptor<BindExternalIdentityCommand> bindingCaptor = ArgumentCaptor.forClass(BindExternalIdentityCommand.class);
        verify(identityUserApi).bindExternalIdentity(bindingCaptor.capture());
        BindExternalIdentityCommand binding = bindingCaptor.getValue();
        assertThat(binding.getUserId()).isEqualTo(42L);
        assertThat(binding.getAppCode()).isEqualTo("admin-app");
        assertThat(binding.getProvider()).isEqualTo("WECOM");
        assertThat(binding.getCorpId()).isEqualTo("corp-a");
        assertThat(binding.getExternalUserId()).isEqualTo("external-a");
        assertThat(binding.getBindSource()).isEqualTo("SELF");
    }

    private CompleteProviderAuthorizationCommand completeCommand() {
        CompleteProviderAuthorizationCommand command = new CompleteProviderAuthorizationCommand();
        command.setState("state");
        command.setCode("authorization-code");
        return command;
    }

    private StartProviderAuthorizationCommand startCommand(ProviderAuthorizationIntent intent) {
        StartProviderAuthorizationCommand command = new StartProviderAuthorizationCommand();
        command.setTenantId("tenant-a");
        command.setAppCode("admin-app");
        command.setProvider(ExternalAuthProvider.WECOM);
        command.setIntent(intent);
        command.setRedirectUri("https://admin.example.com/provider-callback");
        return command;
    }

    private ProviderAuthorizationStore.StatePayload loginState() {
        return new ProviderAuthorizationStore.StatePayload("tenant-a", "admin-app", ExternalAuthProvider.WECOM,
                ProviderAuthorizationIntent.LOGIN, "https://admin.example.com/provider-callback", null);
    }

    private IAuthProviderConfigService.ResolvedProviderConfig config() {
        return new IAuthProviderConfigService.ResolvedProviderConfig(1L, "tenant-a", "admin-app",
                ExternalAuthProvider.WECOM, "client-id", "corp-a", "1000003", "secret",
                List.of("https://admin.example.com/provider-callback"));
    }

    private ExternalIdentityBindingVO binding(Long userId) {
        ExternalIdentityBindingVO binding = new ExternalIdentityBindingVO();
        binding.setUserId(userId);
        return binding;
    }
}
