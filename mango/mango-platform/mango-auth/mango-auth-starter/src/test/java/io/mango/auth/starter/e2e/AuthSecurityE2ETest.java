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
import io.mango.auth.starter.config.AuthSecurityConfig;
import io.mango.auth.starter.controller.AuthController;
import io.mango.common.result.R;
import io.mango.infra.context.starter.TtlExecutorDecorator;
import io.mango.infra.kv.api.IKvStore;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.SecurityPrincipal;
import io.mango.authorization.support.token.JjwtTokenServiceImpl;
import io.mango.authorization.support.autoconfigure.SecurityAutoConfiguration;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.concurrent.ConcurrentHashMap;

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

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        ITokenProvider tokenService(IKvStore kvStore) {
            return new JjwtTokenServiceImpl(kvStore);
        }

        @Bean
        AuthUserProvider authUserProvider(PasswordEncoder passwordEncoder) {
            return new AuthUserProvider() {
                @Override
                public AuthUserInfo getByUsernameForAuth(String username) {
                    if (!"admin".equals(username)) {
                        return null;
                    }
                    AuthUserInfo user = new AuthUserInfo();
                    user.setUserId(1L);
                    user.setUsername("admin");
                    user.setNickname("Administrator");
                    user.setStatus(1);
                    user.setPassword(passwordEncoder.encode("admin123"));
                    return user;
                }

                @Override
                public AuthUserInfo getByIdForAuth(Long userId) {
                    if (!Long.valueOf(1L).equals(userId)) {
                        return null;
                    }
                    AuthUserInfo user = new AuthUserInfo();
                    user.setUserId(1L);
                    user.setUsername("admin");
                    user.setNickname("Administrator");
                    user.setStatus(1);
                    user.setPassword(passwordEncoder.encode("admin123"));
                    return user;
                }
            };
        }

        @Bean
        IAuthorizationProvider authorizationProvider() {
            return query -> Long.valueOf(1L).equals(query.subjectId())
                    ? AuthorizationSnapshot.of(List.of("ROLE_ADMIN"), List.of("e2e:read"), List.of("ROLE_ADMIN", "e2e:read"))
                    : AuthorizationSnapshot.empty();
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
                        || "/auth/refresh".equals(requestUri)
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
