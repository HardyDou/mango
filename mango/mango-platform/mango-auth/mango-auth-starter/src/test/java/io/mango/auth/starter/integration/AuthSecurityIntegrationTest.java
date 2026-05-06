package io.mango.auth.starter.integration;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.common.result.R;
import io.mango.infra.kv.api.IKvStore;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.SecurityPrincipal;
import io.mango.authorization.support.autoconfigure.SecurityAutoConfiguration;
import io.mango.authorization.support.token.JjwtTokenServiceImpl;
import io.mango.auth.starter.config.AuthSecurityConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AuthSecurityIntegrationTest.TestApp.class,
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
@DisplayName("Auth security integration tests")
class AuthSecurityIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ITokenProvider tokenService;

    @Resource
    private TestAuthorizationProvider authorizationProvider;

    @BeforeEach
    void setUp() {
        authorizationProvider.clear();
    }

    @Test
    @DisplayName("valid token with permission should reach secured endpoint and expose security context")
    void validTokenWithPermissionShouldReachSecuredEndpoint() throws Exception {
        authorizationProvider.grant(100L, "demo:read");
        String token = tokenService.generateAccessToken(100L, "alice", Map.of());

        mockMvc.perform(get("/integration/secured")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("100:alice"));
    }

    @Test
    @DisplayName("valid token without permission should return 403")
    void validTokenWithoutPermissionShouldReturn403() throws Exception {
        String token = tokenService.generateAccessToken(101L, "bob", Map.of());

        mockMvc.perform(get("/integration/secured")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"code\":403,\"message\":\"Access denied\"}"));
    }

    @Test
    @DisplayName("missing token should return 401")
    void missingTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/integration/secured"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"code\":401,\"message\":\"Unauthorized\"}"));
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
            SecuredController.class
    })
    static class TestApp {

        @Bean
        TestAuthorizationProvider authorizationProvider() {
            return new TestAuthorizationProvider();
        }

        @Bean
        IKvStore kvStore() {
            return new InMemoryTestKvStore();
        }

        @Bean
        ITokenProvider tokenService(IKvStore kvStore) {
            return new JjwtTokenServiceImpl(kvStore);
        }

        @Bean("apiResourceAuthorizationManager")
        AuthorizationManager<RequestAuthorizationContext> apiResourceAuthorizationManager(
                IAuthorizationProvider authorizationProvider) {
            return (authenticationSupplier, context) -> {
                Authentication authentication = authenticationSupplier.get();
                boolean authenticated = authentication != null
                        && authentication.isAuthenticated()
                        && !(authentication instanceof AnonymousAuthenticationToken);
                if (!authenticated) {
                    return new AuthorizationDecision(false);
                }
                Long userId = ((SecurityPrincipal) authentication.getPrincipal()).userId();
                return new AuthorizationDecision(
                        authorizationProvider.load(AuthorizationQuery.user(userId)).permissionCodes().contains("demo:read"));
            };
        }
    }

    @RestController
    static class SecuredController {

        private final ISecurityContextProvider securityContextProvider;

        SecuredController(ISecurityContextProvider securityContextProvider) {
            this.securityContextProvider = securityContextProvider;
        }

        @GetMapping("/integration/secured")
        public R<String> secured() {
            var context = securityContextProvider.currentContext();
            return R.ok(context.userId() + ":" + context.principalName());
        }
    }

    static class TestAuthorizationProvider implements IAuthorizationProvider {

        private final Map<Long, List<String>> permissions = new ConcurrentHashMap<>();

        @Override
        public AuthorizationSnapshot load(AuthorizationQuery query) {
            return AuthorizationSnapshot.of(List.of(), permissions.getOrDefault(query.subjectId(), List.of()), List.of());
        }

        void grant(Long userId, String permission) {
            permissions.put(userId, List.of(permission));
        }

        void clear() {
            permissions.clear();
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
}
