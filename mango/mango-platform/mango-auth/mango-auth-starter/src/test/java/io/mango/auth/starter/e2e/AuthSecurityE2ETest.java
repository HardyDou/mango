package io.mango.auth.starter.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.spi.LoginTenantProvider;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.auth.core.service.impl.AuthServiceImpl;
import io.mango.auth.core.service.TokenRevocationService;
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
    @DisplayName("login with wrong password should return 401")
    void loginWithWrongPasswordShouldReturn401() throws Exception {
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
                .andExpect(jsonPath("$.code").value(401));
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
                public R<IdentityUserInfo> getUserInfo(String username) {
                    return R.ok("admin".equals(username) ? identityUser() : null);
                }

                @Override
                public R<IdentityUserInfo> getUserInfoById(Long userId) {
                    return R.ok(Long.valueOf(1L).equals(userId) ? identityUser() : null);
                }

                private IdentityUserInfo identityUser() {
                    IdentityUserInfo user = new IdentityUserInfo();
                    user.setUserId(1L);
                    user.setUsername("admin");
                    user.setNickname("Administrator");
                    user.setStatus(1);
                    return user;
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

                private LoginTenantVO tenant() {
                    LoginTenantVO tenant = new LoginTenantVO();
                    tenant.setTenantId("1");
                    tenant.setTenantCode("default");
                    tenant.setTenantName("芒果集团");
                    return tenant;
                }
            };
        }

        @Bean("apiResourceAuthorizationManager")
        AuthorizationManager<RequestAuthorizationContext> apiResourceAuthorizationManager(
                IAuthorizationProvider authorizationProvider) {
            return (authenticationSupplier, context) -> {
                if ("/auth/login".equals(context.getRequest().getRequestURI())) {
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
