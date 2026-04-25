package io.mango.auth.starter.config;

import io.mango.infra.security.api.ISecurityContextProvider;
import io.mango.infra.security.api.ITokenService;
import io.mango.infra.security.starter.SecurityAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthSecurityConfigTest.TestController.class)
@ContextConfiguration(classes = AuthSecurityConfigTest.TestApp.class)
@TestPropertySource(properties = "mango.gateway.auth-enabled=true")
@DisplayName("Auth security config tests")
class AuthSecurityConfigTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private ITokenService tokenService;

    @Test
    @DisplayName("missing bearer token should return 401")
    void missingBearerTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/secure/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":401,\"message\":\"Unauthorized\"}"));
    }

    @Test
    @DisplayName("valid bearer token should populate spring security context")
    void validBearerTokenShouldPopulateSpringSecurityContext() throws Exception {
        Mockito.when(tokenService.validateToken("ok-token")).thenReturn(true);
        Mockito.when(tokenService.getTokenType("ok-token")).thenReturn(ITokenService.TOKEN_TYPE_ACCESS);
        Mockito.when(tokenService.getUserId("ok-token")).thenReturn(99L);
        Mockito.when(tokenService.getUsername("ok-token")).thenReturn("hardy");

        mockMvc.perform(get("/secure/me").header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("99:hardy"));
    }

    @Test
    @DisplayName("whitelist path should bypass authentication")
    void whitelistPathShouldBypassAuthentication() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
        Mockito.verify(tokenService, Mockito.never()).validateToken(anyString());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityAutoConfiguration.class, AuthSecurityConfig.class, TestController.class})
    static class TestApp {
    }

    @RestController
    static class TestController {

        private final ISecurityContextProvider securityContextProvider;

        TestController(ISecurityContextProvider securityContextProvider) {
            this.securityContextProvider = securityContextProvider;
        }

        @GetMapping("/secure/me")
        String me() {
            var context = securityContextProvider.currentContext();
            return context.userId() + ":" + context.principalName();
        }

        @GetMapping("/auth/login")
        String login() {
            return "public";
        }
    }
}
