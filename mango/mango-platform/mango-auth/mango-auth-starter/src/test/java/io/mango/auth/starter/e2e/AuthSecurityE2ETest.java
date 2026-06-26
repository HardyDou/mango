package io.mango.auth.starter.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.AuthCode;
import io.mango.auth.api.spi.LoginTenantProvider;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.auth.core.service.impl.AuthServiceImpl;
import io.mango.auth.core.service.TokenRevocationService;
import io.mango.auth.core.service.WecomLoginClient;
import io.mango.auth.core.service.impl.LoginAttemptTracker;
import io.mango.auth.core.service.impl.PasswordResetTicketService;
import io.mango.auth.starter.config.AuthSecurityConfig;
import io.mango.auth.starter.controller.AuthController;
import io.mango.common.result.R;
import io.mango.infra.context.support.TtlExecutorDecorator;
import io.mango.infra.kv.api.IKvStore;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.SecurityPrincipal;
import io.mango.authorization.support.token.JjwtTokenServiceImpl;
import io.mango.authorization.starter.autoconfigure.SecurityAutoConfiguration;
import io.mango.common.exception.BizException;
import io.mango.identity.api.AuthIdentitySecurityProvider;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AuthSecurityE2ETest.TestApp.class,
        properties = {
                "mango.access.auth-enabled=true",
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
@DisplayName("Auth security E2E tests")
class AuthSecurityE2ETest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private TestUserStore testUserStore;

    @BeforeEach
    void setUp() {
        testUserStore.reset();
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
            AuthServiceImpl.class,
            PasswordResetTicketService.class,
            TokenRevocationService.class,
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
            return new JjwtTokenServiceImpl(kvStore);
        }

        @Bean
        TestUserStore testUserStore(PasswordEncoder passwordEncoder) {
            return new TestUserStore(passwordEncoder);
        }

        @Bean
        AuthUserProvider authUserProvider(TestUserStore userStore) {
            return new AuthUserProvider() {
                @Override
                public AuthUserInfo getByUsernameForAuth(String username) {
                    return userStore.byUsername(username);
                }

                @Override
                public AuthUserInfo getByIdForAuth(Long userId) {
                    return userStore.byId(userId);
                }
            };
        }

        @Bean
        IAuthorizationProvider authorizationProvider() {
            return query -> query.subjectId() != null
                    ? AuthorizationSnapshot.of(List.of("ROLE_ADMIN"), List.of("e2e:read"), List.of("ROLE_ADMIN", "e2e:read"))
                    : AuthorizationSnapshot.empty();
        }

        @Bean
        AuthIdentitySecurityProvider authIdentitySecurityProvider(TestUserStore userStore) {
            return userStore;
        }

        @Bean
        IdentityUserApi identityUserApi() {
            return new IdentityUserApi() {
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
                public R<IdentityUserInfo> getUserInfo(String username) {
                    return R.ok("admin".equals(username) ? identityUser() : null);
                }

                @Override
                public R<IdentityUserInfo> getUserInfoById(Long userId) {
                    return R.ok(Long.valueOf(1L).equals(userId) ? identityUser() : null);
                }

                @Override
                public R<List<IdentityUserInfo>> listUserInfosByTarget(io.mango.identity.api.query.IdentityUserTargetQuery query) {
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

                private IdentityUserInfo identityUser() {
                    IdentityUserInfo user = new IdentityUserInfo();
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
            return (corpId, secret, code) -> "mock-wecom-code".equals(code) ? "wecom-admin" : null;
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
                Long userId = ((SecurityPrincipal) authentication.getPrincipal()).userId();
                return new AuthorizationDecision(
                        authorizationProvider.load(AuthorizationQuery.user(userId)).permissionCodes().contains("e2e:read"));
            };
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

        AuthUserInfo byUsername(String username) {
            Long userId = userIdsByUsername.get(username);
            return userId == null ? null : byId(userId);
        }

        AuthUserInfo byId(Long userId) {
            StoredUser stored = users.get(userId);
            if (stored == null) {
                return null;
            }
            AuthUserInfo user = new AuthUserInfo();
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
        public void assertLoginAllowed(AuthUserInfo user) {
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
