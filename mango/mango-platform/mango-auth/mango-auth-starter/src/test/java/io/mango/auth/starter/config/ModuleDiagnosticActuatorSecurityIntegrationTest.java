package io.mango.auth.starter.config;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.infra.web.support.InternalCallAttributes;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ModuleDiagnosticActuatorSecurityIntegrationTest.TestApp.class,
        properties = {
                "mango.access.auth-enabled=true",
                "mango.module.diagnostics.endpoint.enabled=true",
                "management.endpoints.web.exposure.include=mangoModules",
                "mango.crypto.sm4.secret-key=00112233445566778899aabbccddeeff",
                "server.servlet.context-path=/",
                "spring.application.name=diagnostic-actuator-test",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "io.mango.auth.starter.AuthAutoConfiguration,"
                        + "io.mango.authorization.starter.AuthorizationAutoConfiguration,"
                        + "io.mango.authorization.starter.autoconfigure.SecurityAutoConfiguration,"
                        + "io.mango.authorization.starter.autoconfigure.TokenAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceFlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
class ModuleDiagnosticActuatorSecurityIntegrationTest {

    private static final String PATH = "/actuator/mangoModules";

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ITokenProvider tokenService;

    @Resource
    private IAuthorizationProvider authorizationProvider;

    @BeforeEach
    void setUpToken() {
        Mockito.when(tokenService.validateToken("doctor-token")).thenReturn(true);
        Mockito.when(tokenService.getTokenType("doctor-token")).thenReturn(ITokenProvider.TOKEN_TYPE_ACCESS);
        Mockito.when(tokenService.getUserId("doctor-token")).thenReturn(7L);
        Mockito.when(tokenService.getUsername("doctor-token")).thenReturn("doctor");
        Mockito.when(tokenService.getClaim("doctor-token", "memberId")).thenReturn("70");
        Mockito.when(tokenService.getClaim("doctor-token", "tenantId")).thenReturn("1");
        Mockito.when(tokenService.getClaim("doctor-token", "appCode")).thenReturn("internal-admin");
        Mockito.when(authorizationProvider.load(any(AuthorizationQuery.class)))
                .thenReturn(AuthorizationSnapshotVO.of(
                        List.of(),
                        List.of(ModuleDiagnosticAuthorizationManager.REQUIRED_PERMISSION),
                        List.of()));
    }

    @Test
    void actualActuatorEndpointRequiresDedicatedPermissionAndLoopback() throws Exception {
        mockMvc.perform(get(PATH)
                        .queryParam("module", "mango-link")
                        .queryParam("app", "internal-admin")
                        .queryParam("profile", "ADMIN_MODULE_RUNTIME_V1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportScope").value("INSTANCE_OBSERVATION"))
                .andExpect(jsonPath("$.modules[0].moduleCode").value("mango-link"))
                .andExpect(jsonPath("$.modules[0].conditions[0].durationMs").isNumber());

        mockMvc.perform(get(PATH)
                        .queryParam("module", "mango-link")
                        .queryParam("app", "internal-admin")
                        .queryParam("profile", "ADMIN_MODULE_RUNTIME_V1")
                        .header("Authorization", "Bearer doctor-token")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.8");
                            return request;
                }))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(PATH)
                        .queryParam("module", "mango-link")
                        .queryParam("app", "internal-admin")
                        .queryParam("profile", "ADMIN_MODULE_RUNTIME_V1")
                        .header("Authorization", "Bearer doctor-token")
                        .with(request -> {
                            request.setRemoteAddr("localhost");
                            return request;
                        }))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingOrNonHeaderCredentialsCannotBypassDiagnosticAuthorization() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PATH).queryParam("token", "doctor-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(PATH).cookie(new Cookie("MANGO_TOKEN", "doctor-token")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(PATH).requestAttr(InternalCallAttributes.VERIFIED, Boolean.TRUE))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PATH).header("Authorization", "bearer doctor-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerWithoutDedicatedPermissionIsForbidden() throws Exception {
        Mockito.when(authorizationProvider.load(any(AuthorizationQuery.class)))
                .thenReturn(AuthorizationSnapshotVO.empty());

        mockMvc.perform(get(PATH)
                        .queryParam("app", "internal-admin")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mutationAndMismatchedScopeAreForbidden() throws Exception {
        mockMvc.perform(post(PATH)
                        .queryParam("app", "internal-admin")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(PATH)
                        .queryParam("app", "other-admin")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(PATH)
                        .queryParam("app", "internal-admin", "other-admin")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());

        Mockito.when(tokenService.getClaim("doctor-token", "tenantId")).thenReturn("2");
        mockMvc.perform(get(PATH)
                        .queryParam("app", "internal-admin")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AuthSecurityConfig.class)
    static class TestApp {

        @Bean
        ITokenProvider tokenProvider() {
            return Mockito.mock(ITokenProvider.class);
        }

        @Bean
        IAuthorizationProvider authorizationProvider() {
            return Mockito.mock(IAuthorizationProvider.class);
        }

        @Bean
        AuthenticationEntryPoint authenticationEntryPoint() {
            return (request, response, exception) -> response.sendError(401);
        }

        @Bean
        AccessDeniedHandler accessDeniedHandler() {
            return (request, response, exception) -> response.sendError(403);
        }
    }
}
