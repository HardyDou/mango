package io.mango.auth.starter.config;

import io.mango.infra.security.api.ITokenProvider;
import io.mango.infra.security.starter.SecurityAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthSecurityResourcePolicyTest.TestController.class)
@ContextConfiguration(classes = AuthSecurityResourcePolicyTest.TestApp.class)
@TestPropertySource(properties = "mango.access.auth-enabled=true")
@DisplayName("Auth security resource policy tests")
class AuthSecurityResourcePolicyTest {

    @Resource
    private MockMvc mockMvc;

    @MockBean
    private ITokenProvider tokenService;

    @Test
    @DisplayName("resource policy manager should allow configured public endpoint without token")
    void resourcePolicyShouldAllowPublicEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/policy/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    @DisplayName("resource policy manager should still require authentication for login endpoint")
    void resourcePolicyShouldRequireAuthenticationForLoginEndpoint() throws Exception {
        mockMvc.perform(get("/policy/login"))
                .andExpect(status().isUnauthorized());

        Mockito.when(tokenService.validateToken("ok-token")).thenReturn(true);
        Mockito.when(tokenService.getTokenType("ok-token")).thenReturn(ITokenProvider.TOKEN_TYPE_ACCESS);
        Mockito.when(tokenService.getUserId("ok-token")).thenReturn(7L);
        Mockito.when(tokenService.getUsername("ok-token")).thenReturn("policy-user");

        mockMvc.perform(get("/policy/login").header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("login"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({AuthSecurityConfig.class, SecurityAutoConfiguration.class, TestController.class})
    static class TestApp {

        @Bean("apiResourceAuthorizationManager")
        AuthorizationManager<RequestAuthorizationContext> apiResourceAuthorizationManager() {
            return (authentication, context) -> {
                String path = context.getRequest().getRequestURI();
                if ("/policy/public".equals(path)) {
                    return new AuthorizationDecision(true);
                }
                boolean granted = authentication.get() != null
                        && authentication.get().isAuthenticated()
                        && !(authentication.get() instanceof AnonymousAuthenticationToken);
                return new AuthorizationDecision(granted);
            };
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/policy/public")
        String publicEndpoint() {
            return "public";
        }

        @GetMapping("/policy/login")
        String loginEndpoint() {
            return "login";
        }
    }
}
