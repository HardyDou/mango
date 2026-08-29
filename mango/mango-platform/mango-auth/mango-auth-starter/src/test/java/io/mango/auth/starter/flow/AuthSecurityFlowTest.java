package io.mango.auth.starter.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.api.spi.LoginTenantProvider;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.auth.core.service.impl.AuthService;
import io.mango.auth.core.store.TokenRevocationStore;
import io.mango.auth.core.service.WecomLoginClient;
import io.mango.auth.core.service.IExternalAuthorizationService;
import io.mango.auth.core.service.impl.LoginAttemptTracker;
import io.mango.auth.core.store.PasswordResetTicketStore;
import io.mango.auth.starter.config.AuthSecurityConfig;
import io.mango.auth.starter.controller.AuthController;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaTypesResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.common.result.R;
import io.mango.infra.context.support.TtlExecutorDecorator;
import io.mango.infra.kv.api.IKvStore;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.vo.SecurityPrincipalVO;
import io.mango.authorization.support.token.JjwtTokenProvider;
import io.mango.authorization.starter.autoconfigure.SecurityAutoConfiguration;
import io.mango.common.exception.BizException;
import io.mango.identity.api.AuthIdentitySecurityProvider;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.notice.api.command.NoticeSendEventCommand;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AuthSecurityFlowTest.TestApp.class,
        properties = {
                "mango.access.auth-enabled=true",
                "mango.crypto.enabled=false",
                "mango.security.jwt.secret=mango-secret-key-for-jwt-token-generation-must-be-at-least-256-bits",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceFlywayAutoConfiguration,"
                        + "io.mango.authorization.starter.AuthorizationAutoConfiguration,"
                        + "com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure"
        })
@AutoConfigureMockMvc
@RecordApplicationEvents
@Tag("flow")
@Tag("auth")
@DisplayName("Auth security component flow tests")
class AuthSecurityFlowTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private TestUserStore testUserStore;

    @Resource
    private TestCaptchaApi testCaptchaApi;

    @Resource
    private ApplicationEvents applicationEvents;

    @Resource
    private IExternalAuthorizationService externalAuthorizationService;

    @BeforeEach
    void setUp() {
        testUserStore.reset();
        testCaptchaApi.reset();
        org.mockito.Mockito.reset(externalAuthorizationService);
    }

    @Test
    @DisplayName("login should issue token and token should access secured endpoint")
    void loginShouldIssueTokenAndAccessSecuredEndpoint() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123",
                                  "tenantId": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = body.path("data").path("accessToken").asText();

        mockMvc.perform(get("/e2e/secured")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("1:admin"));

        assertThat(applicationEvents.stream(NoticeSendEventCommand.class).toList())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getTenantId()).isEqualTo("1");
                    assertThat(event.getBizType()).isEqualTo("auth.login.success");
                    assertThat(event.getMessageTarget().getTargetType()).isEqualTo(NoticeSiteMessageTargetType.ROUTE);
                    assertThat(event.getMessageTarget().getTargetKey()).isEqualTo("account:profile");
                    assertThat(event.getMessageActions()).singleElement()
                            .satisfies(action -> assertThat(action.getActionCode()).isEqualTo("VIEW_PROFILE"));
                });
    }

    @Test
    @DisplayName("auth info should preserve party context carried by the access token")
    void authInfoShouldPreserveTokenPartyContext() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123",
                                  "tenantId": "1",
                                  "partyType": "INTERNAL_ORG",
                                  "partyId": 42
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partyId").value("42"))
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(get("/auth/info")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partyType").value("INTERNAL_ORG"))
                .andExpect(jsonPath("$.data.partyId").value("42"))
                .andExpect(jsonPath("$.data.departmentName").value("技术研发部"))
                .andExpect(jsonPath("$.data.companyName").value("芒果集团"));
    }

    @Test
    @DisplayName("WeCom profile refresh should require login and use the current account")
    void wecomProfileRefreshShouldRequireLogin() throws Exception {
        mockMvc.perform(post("/auth/providers/wecom/profile/refresh"))
                .andExpect(status().isUnauthorized());

        org.mockito.Mockito.when(externalAuthorizationService.refreshCurrentWecomProfile()).thenReturn(true);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123",
                                  "tenantId": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(post("/auth/providers/wecom/profile/refresh")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));

        org.mockito.Mockito.verify(externalAuthorizationService).refreshCurrentWecomProfile();
    }

    @Test
    @DisplayName("public captcha send should preserve the captcha unified response")
    void publicCaptchaSendShouldPreserveCaptchaUnifiedResponse() throws Exception {
        mockMvc.perform(post("/auth/captcha/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SMS",
                                  "target": "13800138000",
                                  "businessType": "LOGIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("captcha:LOGIN:13800138000"));
    }

    @Test
    @DisplayName("public captcha send should preserve a downstream business failure")
    void publicCaptchaSendShouldPreserveDownstreamBusinessFailure() throws Exception {
        testCaptchaApi.respondWith(R.fail(AuthCode.CAPTCHA_INVALID));

        mockMvc.perform(post("/auth/captcha/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SMS",
                                  "target": "13800138000",
                                  "businessType": "LOGIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(AuthCode.CAPTCHA_INVALID.getCode()))
                .andExpect(jsonPath("$.msg").value(AuthCode.CAPTCHA_INVALID.getMessage()));
    }

    @Test
    @DisplayName("WeCom login should issue token for bound identity")
    void wecomLoginShouldIssueTokenForBoundIdentity() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/wecom/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "mock-wecom-code",
                                  "channelConfigId": 1,
                                  "tenantId": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = body.path("data").path("accessToken").asText();

        mockMvc.perform(get("/e2e/secured")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("1:admin"));
    }

    @Test
    @DisplayName("login with wrong password should return auth business code")
    void loginWithWrongPasswordShouldReturnAuthBusinessCode() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong-password",
                                  "tenantId": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID.getCode()))
                .andExpect(jsonPath("$.msg").value(AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID.getMessage()));
    }

    @Test
    @DisplayName("first login should require password change and changed password should issue token")
    void firstLoginShouldRequirePasswordChangeAndThenIssueToken() throws Exception {
        MvcResult firstLogin = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "first-login",
                                  "password": "Init@123456",
                                  "tenantId": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.passwordResetRequired").value(true))
                .andExpect(jsonPath("$.data.loginAction").value("CHANGE_PASSWORD"))
                .andExpect(jsonPath("$.data.passwordResetTicket").isNotEmpty())
                .andReturn();

        JsonNode firstBody = objectMapper.readTree(firstLogin.getResponse().getContentAsString());
        String ticket = firstBody.path("data").path("passwordResetTicket").asText();

        mockMvc.perform(post("/auth/password/change-required")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passwordResetTicket": "%s",
                                  "newPassword": "12345678",
                                  "confirmPassword": "12345678"
                                }
                                """.formatted(ticket)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        MvcResult changed = mockMvc.perform(post("/auth/password/change-required")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passwordResetTicket": "%s",
                                  "newPassword": "Changed@123456",
                                  "confirmPassword": "Changed@123456"
                                }
                                """.formatted(ticket)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.passwordResetRequired").value(false))
                .andReturn();

        String accessToken = objectMapper.readTree(changed.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        mockMvc.perform(get("/e2e/secured")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("2:first-login"));

        mockMvc.perform(post("/auth/password/change-required")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passwordResetTicket": "%s",
                                  "newPassword": "Changed@654321",
                                  "confirmPassword": "Changed@654321"
                                }
                                """.formatted(ticket)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(AuthCode.PASSWORD_RESET_TICKET_INVALID.getCode()));
    }

    @Test
    @DisplayName("failed login should lock account and admin unlock should restore login")
    void failedLoginShouldLockAccountAndAdminUnlockShouldRestoreLogin() throws Exception {
        mockMvc.perform(post("/auth/login-institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "lock-user",
                                  "realm": "INTERNAL",
                                  "appCode": "internal-admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "lock-user",
                                      "password": "wrong-password",
                                      "tenantId": "1"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "lock-user",
                                  "password": "Lock@123456",
                                  "tenantId": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(AuthCode.LOGIN_ATTEMPT_LOCKED.getCode()));

        assertThat(applicationEvents.stream(NoticeSendEventCommand.class).toList())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getTenantId()).isEqualTo("1");
                    assertThat(event.getBizType()).isEqualTo("auth.login.locked");
                    assertThat(event.getMessageSubject().getSubjectType()).isEqualTo("AUTH_LOGIN_LOCK");
                    assertThat(event.getMessageTarget().getTargetKey()).isEqualTo("system:user");
                    assertThat(event.getMessageActions()).singleElement()
                            .satisfies(action -> assertThat(action.getActionCode()).isEqualTo("VIEW_USER"));
                });

        testUserStore.unlock(3L);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "lock-user",
                                  "password": "Lock@123456",
                                  "tenantId": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("unknown username failures should be locked by kv marker")
    void unknownUsernameFailuresShouldBeLockedByKvMarker() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "admin1",
                                      "password": "wrong-password",
                                      "tenantId": "1",
                                      "realm": "INTERNAL"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(AuthCode.LOGIN_ACCOUNT_OR_PASSWORD_INVALID.getCode()));
        }

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin1",
                                  "password": "wrong-password",
                                  "tenantId": "1",
                                  "realm": "INTERNAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(AuthCode.LOGIN_ATTEMPT_LOCKED.getCode()));
    }

    @Test
    @DisplayName("refresh with invalid token should return refresh business code")
    void refreshWithInvalidTokenShouldReturnRefreshBusinessCode() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "invalid-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(AuthCode.REFRESH_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.msg").value(AuthCode.REFRESH_TOKEN_INVALID.getMessage()));
    }

    @Test
    @DisplayName("logout should revoke current access token")
    void logoutShouldRevokeCurrentAccessToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123",
                                  "tenantId": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = body.path("data").path("accessToken").asText();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + accessToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/e2e/secured")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "io.mango.auth.starter.AuthAutoConfiguration",
            "io.mango.infra.kv.starter.redis.KvRedisAutoConfiguration",
            "io.mango.infra.kv.starter.KvStoreAutoConfiguration"
    })
    @Import({
            AuthSecurityConfig.class,
            SecurityAutoConfiguration.class,
            AuthController.class,
            AuthService.class,
            PasswordResetTicketStore.class,
            TokenRevocationStore.class,
            SecuredController.class
    })
    static class TestApp {

        @Bean
        TtlExecutorDecorator ttlExecutorDecorator() {
            return new TtlExecutorDecorator();
        }

        @Bean
        IKvStore kvStore() {
            return new InMemoryTestKvStore();
        }

        @Bean(destroyMethod = "shutdown")
        LoginAttemptTracker loginAttemptTracker(IKvStore kvStore) {
            return new LoginAttemptTracker(kvStore, Executors.newSingleThreadScheduledExecutor(), 5, 60, 15);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        ITokenProvider tokenService(IKvStore kvStore) {
            return new JjwtTokenProvider(kvStore);
        }

        @Bean
        TestUserStore testUserStore(PasswordEncoder passwordEncoder) {
            return new TestUserStore(passwordEncoder);
        }

        @Bean
        io.mango.auth.core.service.IAuthProviderConfigService authProviderConfigService() {
            io.mango.auth.core.service.IAuthProviderConfigService service = org.mockito.Mockito.mock(
                    io.mango.auth.core.service.IAuthProviderConfigService.class);
            io.mango.auth.core.service.IAuthProviderConfigService.ResolvedProviderConfig config =
                    new io.mango.auth.core.service.IAuthProviderConfigService.ResolvedProviderConfig(
                            1L, "1", "internal-admin", io.mango.auth.api.enums.ExternalAuthProvider.WECOM,
                            "mock-corp", "mock-corp", "1000003", "mock-secret",
                            List.of("http://127.0.0.1:8550/login"));
            org.mockito.Mockito.when(service.requireAvailable(
                            org.mockito.ArgumentMatchers.any(
                                    io.mango.auth.core.service.IAuthProviderConfigService.ProviderSelection.class)))
                    .thenReturn(config);
            return service;
        }

        @Bean
        io.mango.auth.core.service.IExternalAuthorizationService externalAuthorizationService() {
            return org.mockito.Mockito.mock(io.mango.auth.core.service.IExternalAuthorizationService.class);
        }

        @Bean
        AuthUserProvider authUserProvider(TestUserStore userStore) {
            return new AuthUserProvider() {
                @Override
                public AuthUserVO getByUsernameForAuth(String username) {
                    return userStore.byUsername(username);
                }

                @Override
                public AuthUserVO getByIdForAuth(Long userId) {
                    return userStore.byId(userId);
                }
            };
        }

        @Bean
        IAuthorizationProvider authorizationProvider() {
            return query -> query.subjectId() != null
                    ? AuthorizationSnapshotVO.of(List.of("ROLE_ADMIN"), List.of("e2e:read"), List.of("ROLE_ADMIN", "e2e:read"))
                    : AuthorizationSnapshotVO.empty();
        }

        @Bean
        AuthIdentitySecurityProvider authIdentitySecurityProvider(TestUserStore userStore) {
            return userStore;
        }

        @Bean
        IdentityUserApi identityUserApi() {
            return new IdentityUserApi() {
                @Override
                public R<io.mango.identity.api.vo.CurrentUserProfileVO> currentProfile() {
                    return R.ok(null);
                }

                @Override
                public R<io.mango.identity.api.vo.CurrentUserProfileVO> updateCurrentProfile(
                        io.mango.identity.api.command.UpdateCurrentUserProfileCommand command) {
                    return R.ok(null);
                }

                @Override
                public R<io.mango.identity.api.vo.ContactCaptchaTicketVO> sendCurrentContactCaptcha(
                        io.mango.identity.api.command.SendContactCaptchaCommand command) {
                    return R.ok(null);
                }

                @Override
                public R<io.mango.identity.api.vo.CurrentUserProfileVO> updateCurrentContact(
                        io.mango.identity.api.command.UpdateCurrentUserContactCommand command) {
                    return R.ok(null);
                }

                @Override
                public R<io.mango.common.vo.PageResult<io.mango.identity.api.vo.IdentityUserVO>> page(
                        io.mango.identity.api.query.IdentityUserPageQuery query) {
                    return R.ok(io.mango.common.vo.PageResult.of(List.of(), 0, 1, 10));
                }

                @Override
                public R<io.mango.identity.api.vo.IdentityUserVO> detail(Long userId) {
                    return R.ok(null);
                }

                @Override
                public R<Long> create(io.mango.identity.api.command.CreateIdentityUserCommand command) {
                    return R.ok(1L);
                }

                @Override
                public R<Boolean> update(io.mango.identity.api.command.UpdateIdentityUserCommand command) {
                    return R.ok(true);
                }

                @Override
                public R<Boolean> delete(Long userId) {
                    return R.ok(true);
                }

                @Override
                public R<Integer> deleteBatch(io.mango.identity.api.command.BatchDeleteIdentityUserCommand command) {
                    return R.ok(0);
                }

                @Override
                public R<Boolean> updateStatus(io.mango.identity.api.command.UpdateIdentityUserStatusCommand command) {
                    return R.ok(true);
                }

                @Override
                public R<Boolean> resetPassword(io.mango.identity.api.command.ResetIdentityUserPasswordCommand command) {
                    return R.ok(true);
                }

                @Override
                public R<Boolean> unlock(io.mango.identity.api.command.UnlockIdentityUserCommand command) {
                    return R.ok(true);
                }

                @Override
                public R<Boolean> requirePasswordReset(
                        io.mango.identity.api.command.RequireIdentityUserPasswordResetCommand command) {
                    return R.ok(true);
                }

                @Override
                public R<IdentityUserInfoVO> getUserInfo(String username) {
                    return R.ok("admin".equals(username) ? identityUser() : null);
                }

                @Override
                public R<IdentityUserInfoVO> getUserInfoById(Long userId) {
                    return R.ok(Long.valueOf(1L).equals(userId) ? identityUser() : null);
                }

                @Override
                public R<List<IdentityUserInfoVO>> listUserInfos(
                        io.mango.identity.api.request.IdentityUserBatchRequest query) {
                    return R.ok(List.of());
                }

                @Override
                public R<List<IdentityUserInfoVO>> listUserInfosByTarget(io.mango.identity.api.query.IdentityUserTargetQuery query) {
                    return R.ok(query != null && Long.valueOf(1L).equals(query.getTargetId())
                            ? List.of(identityUser()) : List.of());
                }

                @Override
                public R<io.mango.identity.api.vo.ExternalIdentityBindingVO> bindExternalIdentity(
                        io.mango.identity.api.command.BindExternalIdentityCommand command) {
                    return R.ok(null);
                }

                @Override
                public R<Boolean> unbindExternalIdentity(io.mango.identity.api.command.UnbindExternalIdentityCommand command) {
                    return R.ok(true);
                }

                @Override
                public R<io.mango.identity.api.vo.ExternalIdentityBindingVO> findExternalIdentity(
                        io.mango.identity.api.query.ExternalIdentityQuery query) {
                    if (query != null
                            && "WECOM".equals(query.getProvider())
                            && "mock-corp".equals(query.getCorpId())
                            && "wecom-admin".equals(query.getExternalUserId())) {
                        io.mango.identity.api.vo.ExternalIdentityBindingVO binding =
                                new io.mango.identity.api.vo.ExternalIdentityBindingVO();
                        binding.setUserId(1L);
                        binding.setProvider("WECOM");
                        binding.setCorpId("mock-corp");
                        binding.setExternalUserId("wecom-admin");
                        binding.setBindStatus("BOUND");
                        return R.ok(binding);
                    }
                    return R.ok(null);
                }

                @Override
                public R<List<io.mango.identity.api.vo.ExternalIdentityBindingVO>> listExternalIdentities(Long userId) {
                    return R.ok(List.of());
                }

                @Override
                public R<List<io.mango.identity.api.vo.ExternalIdentityBindingVO>> listCurrentExternalIdentities() {
                    return R.ok(List.of());
                }

                @Override
                public R<Boolean> unbindCurrentExternalIdentity(
                        io.mango.identity.api.command.UnbindCurrentExternalIdentityCommand command) {
                    return R.ok(true);
                }

                private IdentityUserInfoVO identityUser() {
                    IdentityUserInfoVO user = new IdentityUserInfoVO();
                    user.setUserId(1L);
                    user.setUsername("admin");
                    user.setNickname("Administrator");
                    user.setStatus(1);
                    user.setPartyType("INTERNAL_ORG");
                    user.setPartyId(1L);
                    return user;
                }
            };
        }

        @Bean
        io.mango.identity.api.TenantMemberProvider tenantMemberProvider() {
            io.mango.identity.api.TenantMemberProvider provider = org.mockito.Mockito.mock(
                    io.mango.identity.api.TenantMemberProvider.class);
            io.mango.identity.api.vo.TenantMemberVO member = new io.mango.identity.api.vo.TenantMemberVO();
            member.setMemberId(1L);
            member.setTenantId(1L);
            member.setPrimaryOrgId(101L);
            org.mockito.Mockito.when(provider.getMember(1L)).thenReturn(member);
            return provider;
        }

        @Bean
        io.mango.org.api.OrgReferenceProvider orgReferenceProvider() {
            io.mango.org.api.OrgReferenceProvider provider = org.mockito.Mockito.mock(
                    io.mango.org.api.OrgReferenceProvider.class);
            org.mockito.Mockito.when(provider.resolveOrgName(1L, 101L)).thenReturn("技术研发部");
            return provider;
        }

        @Bean
        TestCaptchaApi captchaApi() {
            return new TestCaptchaApi();
        }

        @Bean
        NoticeApi noticeApi() {
            NoticeApi api = org.mockito.Mockito.mock(NoticeApi.class);
            NoticeWecomLoginConfigVO config = new NoticeWecomLoginConfigVO();
            config.setChannelConfigId(1L);
            config.setCorpId("mock-corp");
            config.setAgentId("1000003");
            config.setSecret("mock-secret");
            config.setRedirectUri("http://127.0.0.1:8550/login");
            org.mockito.Mockito.when(api.getWecomLoginConfig(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(R.ok(config));
            return api;
        }

        @Bean
        WecomLoginClient wecomLoginClient() {
            return new WecomLoginClient() {
                @Override
                public String getUserId(String corpId, String secret, String code) {
                    return "mock-wecom-code".equals(code) ? "wecom-admin" : null;
                }

                @Override
                public WecomUserProfile getUserProfile(String corpId, String secret, String userId) {
                    return new WecomUserProfile(userId, "企业微信管理员", null);
                }
            };
        }

        @Bean
        LoginTenantProvider loginTenantProvider() {
            return new LoginTenantProvider() {
                @Override
                public LoginTenantVO getEnabledById(String tenantId) {
                    return "1".equals(tenantId) ? tenant() : null;
                }

                @Override
                public LoginTenantVO getEnabledByCode(String tenantCode) {
                    return "default".equals(tenantCode) ? tenant() : null;
                }

                @Override
                public List<LoginTenantVO> listEnabledByUser(Long userId) {
                    return List.of(tenant());
                }

                private LoginTenantVO tenant() {
                    LoginTenantVO tenant = new LoginTenantVO();
                    tenant.setTenantId("1");
                    tenant.setTenantCode("default");
                    tenant.setTenantName("芒果集团");
                    tenant.setMemberId(1L);
                    return tenant;
                }
            };
        }

        @Bean("apiResourceAuthorizationManager")
        AuthorizationManager<RequestAuthorizationContext> apiResourceAuthorizationManager(
                IAuthorizationProvider authorizationProvider) {
            return (authenticationSupplier, context) -> {
                String requestUri = context.getRequest().getRequestURI();
                if ("/auth/login".equals(requestUri)
                        || "/auth/captcha/send".equals(requestUri)
                        || "/auth/login-institutions".equals(requestUri)
                        || "/auth/refresh".equals(requestUri)
                        || "/auth/password/change-required".equals(requestUri)
                        || "/auth/wecom/login".equals(requestUri)
                        || "/auth/wecom/login-config".equals(requestUri)) {
                    return new AuthorizationDecision(true);
                }
                var authentication = authenticationSupplier.get();
                boolean authenticated = authentication != null
                        && authentication.isAuthenticated()
                        && !(authentication instanceof AnonymousAuthenticationToken);
                if (!authenticated) {
                    return new AuthorizationDecision(false);
                }
                Long userId = ((SecurityPrincipalVO) authentication.getPrincipal()).userId();
                return new AuthorizationDecision(
                        authorizationProvider.load(AuthorizationQuery.user(userId)).permissionCodes().contains("e2e:read"));
            };
        }

    }

    static class TestCaptchaApi implements CaptchaApi {

        private R<String> sendResult;

        void reset() {
            sendResult = null;
        }

        void respondWith(R<String> result) {
            sendResult = result;
        }

        @Override
        public R<CaptchaTypesResponse> getTypes() {
            return R.ok(new CaptchaTypesResponse());
        }

        @Override
        public R<CaptchaResponse> generateArithmetic() {
            return R.ok(new CaptchaResponse());
        }

        @Override
        public R<CaptchaResponse> generateBlockPuzzle() {
            return R.ok(new CaptchaResponse());
        }

        @Override
        public R<CaptchaResponse> generateClickWord() {
            return R.ok(new CaptchaResponse());
        }

        @Override
        public R<CaptchaResponse> generateBehavior() {
            return R.ok(new CaptchaResponse());
        }

        @Override
        public R<BehaviorCaptchaVerifyResponse> verifyBehavior(CaptchaVerifyRequest request) {
            return R.ok(new BehaviorCaptchaVerifyResponse());
        }

        @Override
        public R<Boolean> verify(CaptchaVerifyRequest request) {
            return R.ok(Boolean.TRUE);
        }

        @Override
        public R<String> send(CaptchaSendRequest request) {
            return sendResult == null
                    ? R.ok("captcha:" + request.getBusinessType() + ":" + request.getTarget())
                    : sendResult;
        }
    }

    static final class TestUserStore implements AuthIdentitySecurityProvider {
        private static final int MAX_FAILED_ATTEMPTS = 5;
        private final PasswordEncoder passwordEncoder;
        private final Map<Long, StoredUser> users = new ConcurrentHashMap<>();
        private final Map<String, Long> userIdsByUsername = new ConcurrentHashMap<>();

        TestUserStore(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
            reset();
        }

        void reset() {
            users.clear();
            userIdsByUsername.clear();
            add(new StoredUser(1L, "admin", "admin123", false));
            add(new StoredUser(2L, "first-login", "Init@123456", true));
            add(new StoredUser(3L, "lock-user", "Lock@123456", false));
        }

        AuthUserVO byUsername(String username) {
            Long userId = userIdsByUsername.get(username);
            return userId == null ? null : byId(userId);
        }

        AuthUserVO byId(Long userId) {
            StoredUser stored = users.get(userId);
            if (stored == null) {
                return null;
            }
            AuthUserVO user = new AuthUserVO();
            user.setUserId(stored.userId);
            user.setUsername(stored.username);
            user.setNickname(stored.username);
            user.setStatus(1);
            user.setPassword(stored.encodedPassword);
            user.setRealm("INTERNAL");
            user.setActorType("INTERNAL_USER");
            user.setPartyType("INTERNAL_ORG");
            user.setPartyId(1L);
            user.setPasswordResetRequired(stored.passwordResetRequired);
            user.setFailedLoginCount(stored.failedLoginCount);
            user.setLockedUntil(stored.locked ? LocalDateTime.now().plusMinutes(15) : null);
            return user;
        }

        @Override
        public void assertLoginAllowed(AuthUserVO user) {
            StoredUser stored = users.get(user.getUserId());
            if (stored != null && stored.locked) {
                throw new BizException(AuthCode.LOGIN_ATTEMPT_LOCKED.getCode(), AuthCode.LOGIN_ATTEMPT_LOCKED.getMessage());
            }
        }

        @Override
        public void recordLoginFailure(Long userId) {
            StoredUser stored = users.get(userId);
            if (stored == null) {
                return;
            }
            stored.failedLoginCount++;
            if (stored.failedLoginCount >= MAX_FAILED_ATTEMPTS) {
                stored.locked = true;
            }
        }

        @Override
        public void recordLoginSuccess(Long userId) {
            unlock(userId);
        }

        @Override
        public void changeRequiredPassword(io.mango.identity.api.command.ChangeRequiredPasswordCommand command) {
            StoredUser stored = users.get(command.getUserId());
            if (stored == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            if (!Objects.equals(command.getNewPassword(), command.getConfirmPassword())) {
                throw new IllegalArgumentException("两次输入的新密码不一致");
            }
            validatePassword(command.getNewPassword());
            stored.encodedPassword = passwordEncoder.encode(command.getNewPassword());
            stored.passwordResetRequired = false;
            unlock(command.getUserId());
        }

        private void validatePassword(String password) {
            if (password == null || password.length() < 8) {
                throw new IllegalArgumentException("密码长度不能少于8位");
            }
            if (password.matches(".*\\s+.*")) {
                throw new IllegalArgumentException("密码不能包含空白字符");
            }
            if (!password.matches(".*[A-Za-z].*")) {
                throw new IllegalArgumentException("密码必须包含字母");
            }
            if (!password.matches(".*\\d.*")) {
                throw new IllegalArgumentException("密码必须包含数字");
            }
        }

        void unlock(Long userId) {
            StoredUser stored = users.get(userId);
            if (stored != null) {
                stored.failedLoginCount = 0;
                stored.locked = false;
            }
        }

        private void add(StoredUser user) {
            user.encodedPassword = passwordEncoder.encode(user.rawPassword);
            users.put(user.userId, user);
            userIdsByUsername.put(user.username, user.userId);
        }
    }

    static final class StoredUser {
        final Long userId;
        final String username;
        final String rawPassword;
        String encodedPassword;
        boolean passwordResetRequired;
        int failedLoginCount;
        boolean locked;

        StoredUser(Long userId, String username, String rawPassword, boolean passwordResetRequired) {
            this.userId = userId;
            this.username = username;
            this.rawPassword = rawPassword;
            this.passwordResetRequired = passwordResetRequired;
        }
    }

    static class InMemoryTestKvStore implements IKvStore {

        private final Map<String, String> values = new ConcurrentHashMap<>();

        @Override
        public boolean setIfAbsent(String key, String value, long expireSeconds) {
            return values.putIfAbsent(key, value) == null;
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public long increment(String key, long windowSeconds) {
            return Long.parseLong(values.merge(key, "1", (current, ignored) -> String.valueOf(Long.parseLong(current) + 1)));
        }

        @Override
        public void delete(String key) {
            values.remove(key);
        }

        @Override
        public boolean exists(String key) {
            return values.containsKey(key);
        }
    }

    @RestController
    static class SecuredController {

        private final ISecurityContextProvider securityContextProvider;

        SecuredController(ISecurityContextProvider securityContextProvider) {
            this.securityContextProvider = securityContextProvider;
        }

        @GetMapping("/e2e/secured")
        public R<String> secured() {
            var context = securityContextProvider.currentContext();
            return R.ok(context.userId() + ":" + context.principalName());
        }
    }
}
