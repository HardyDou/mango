package io.mango.auth.starter.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.auth.core.service.impl.AuthServiceImpl;
import io.mango.auth.starter.config.AuthSecurityConfig;
import io.mango.auth.starter.controller.AuthController;
import io.mango.common.result.R;
import io.mango.infra.context.starter.TtlExecutorDecorator;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.security.api.IPermissionService;
import io.mango.infra.security.api.ISecurityContextProvider;
import io.mango.infra.security.api.ITokenService;
import io.mango.infra.security.api.Perm;
import io.mango.infra.security.core.impl.JjwtTokenServiceImpl;
import io.mango.infra.security.starter.SecurityAutoConfiguration;
import io.mango.identity.api.IAuthUserProvider;
import io.mango.identity.api.vo.AuthUserInfo;
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
                "mango.gateway.auth-enabled=true",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,"
                        + "io.mango.infra.db.starter.MangoFlywayAutoConfiguration,"
                        + "io.mango.authorization.starter.AuthorizationAutoConfiguration,"
                        + "io.mango.authorization.starter.config.AuthorizationSecurityAdapterAutoConfiguration,"
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
                                  "password": "admin123"
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
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "io.mango.auth.starter.AuthAutoConfiguration",
            "io.mango.infra.redis.starter.RedisAutoConfiguration",
            "io.mango.infra.kv.starter.KvStoreAutoConfiguration"
    })
    @Import({
            SecurityAutoConfiguration.class,
            AuthSecurityConfig.class,
            AuthController.class,
            AuthServiceImpl.class,
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
        ITokenService tokenService() {
            JjwtTokenServiceImpl impl = new JjwtTokenServiceImpl(null);
            setField(impl, "newSecret", "mango-secret-key-for-jwt-token-generation-must-be-at-least-256-bits");
            setField(impl, "legacySecret", "");
            setField(impl, "accessTokenValiditySeconds", 7200L);
            setField(impl, "refreshTokenValiditySeconds", 604800L);
            impl.init();
            return impl;
        }

        @Bean
        IAuthUserProvider authUserProvider(PasswordEncoder passwordEncoder) {
            return new IAuthUserProvider() {
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
        IPermissionService permissionService(IAuthorizationProvider authorizationProvider) {
            return userId -> authorizationProvider.load(AuthorizationQuery.user(userId)).permissionCodes().stream().toList();
        }

        private static void setField(Object target, String fieldName, Object value) {
            try {
                var field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
        @Perm("e2e:read")
        public R<String> secured() {
            var context = securityContextProvider.currentContext();
            return R.ok(context.userId() + ":" + context.principalName());
        }
    }
}
