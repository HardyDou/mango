package io.mango.auth.starter.integration;

import io.mango.common.result.R;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.security.api.IPermissionProvider;
import io.mango.infra.security.api.ISecurityContextProvider;
import io.mango.infra.security.api.ITokenProvider;
import io.mango.infra.security.api.Perm;
import io.mango.infra.security.starter.SecurityAutoConfiguration;
import io.mango.infra.security.core.impl.JjwtTokenServiceImpl;
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
                        + "io.mango.authorization.starter.config.AuthorizationSecurityAdapterAutoConfiguration,"
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
    private TestPermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService.clear();
    }

    @Test
    @DisplayName("valid token with permission should reach secured endpoint and expose security context")
    void validTokenWithPermissionShouldReachSecuredEndpoint() throws Exception {
        permissionService.grant(100L, "demo:read");
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
            "io.mango.infra.redis.starter.RedisAutoConfiguration",
            "io.mango.infra.kv.starter.KvStoreAutoConfiguration"
    })
    @Import({
            AuthSecurityConfig.class,
            SecurityAutoConfiguration.class,
            SecuredController.class
    })
    static class TestApp {

        @Bean
        TestPermissionService permissionService() {
            return new TestPermissionService();
        }

        @Bean
        IKvStore kvStore() {
            return new InMemoryTestKvStore();
        }

        @Bean
        ITokenProvider tokenService(IKvStore kvStore) {
            return new JjwtTokenServiceImpl(kvStore);
        }
    }

    @RestController
    static class SecuredController {

        private final ISecurityContextProvider securityContextProvider;

        SecuredController(ISecurityContextProvider securityContextProvider) {
            this.securityContextProvider = securityContextProvider;
        }

        @GetMapping("/integration/secured")
        @Perm("demo:read")
        public R<String> secured() {
            var context = securityContextProvider.currentContext();
            return R.ok(context.userId() + ":" + context.principalName());
        }
    }

    static class TestPermissionService implements IPermissionProvider {

        private final Map<Long, List<String>> permissions = new ConcurrentHashMap<>();

        @Override
        public List<String> listUserPermissions(Long userId) {
            return permissions.getOrDefault(userId, List.of());
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
