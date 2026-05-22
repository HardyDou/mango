package io.mango.auth.starter.config;

import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.support.autoconfigure.SecurityAutoConfiguration;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthSecurityConfigTest.TestController.class)
@ContextConfiguration(classes = AuthSecurityConfigTest.TestApp.class)
@TestPropertySource(properties = "mango.access.auth-enabled=true")
@DisplayName("Auth security config tests")
class AuthSecurityConfigTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private ITokenProvider tokenService;

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
        Mockito.when(tokenService.getTokenType("ok-token")).thenReturn(ITokenProvider.TOKEN_TYPE_ACCESS);
        Mockito.when(tokenService.getUserId("ok-token")).thenReturn(99L);
        Mockito.when(tokenService.getUsername("ok-token")).thenReturn("hardy");

        mockMvc.perform(get("/secure/me").header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("99:hardy"));
    }

    @Test
    @DisplayName("valid cookie token should populate spring security context")
    void validCookieTokenShouldPopulateSpringSecurityContext() throws Exception {
        Mockito.when(tokenService.validateToken("cookie-token")).thenReturn(true);
        Mockito.when(tokenService.getTokenType("cookie-token")).thenReturn(ITokenProvider.TOKEN_TYPE_ACCESS);
        Mockito.when(tokenService.getUserId("cookie-token")).thenReturn(101L);
        Mockito.when(tokenService.getUsername("cookie-token")).thenReturn("cookie-user");

        mockMvc.perform(get("/secure/me").cookie(new Cookie("MANGO_TOKEN", "cookie-token")))
                .andExpect(status().isOk())
                .andExpect(content().string("101:cookie-user"));
    }

    @Test
    @DisplayName("valid bearer token should include tenant claim")
    void validBearerTokenShouldIncludeTenantClaim() throws Exception {
        Mockito.when(tokenService.validateToken("tenant-token")).thenReturn(true);
        Mockito.when(tokenService.getTokenType("tenant-token")).thenReturn(ITokenProvider.TOKEN_TYPE_ACCESS);
        Mockito.when(tokenService.getUserId("tenant-token")).thenReturn(100L);
        Mockito.when(tokenService.getUsername("tenant-token")).thenReturn("tenant-user");
        Mockito.when(tokenService.getClaim("tenant-token", "tenantId")).thenReturn("tenant-a");

        mockMvc.perform(get("/secure/tenant").header("Authorization", "Bearer tenant-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("tenant-a"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({AuthSecurityConfig.class, SecurityAutoConfiguration.class, TestController.class})
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

        @GetMapping("/secure/tenant")
        String tenant() {
            return securityContextProvider.currentContext().tenantId();
        }

        @GetMapping("/auth/login")
        String login() {
            return "public";
        }
    }
}
