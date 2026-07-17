package io.mango.authorization.starter.autoconfigure;

import io.mango.infra.web.support.InternalCallAttributes;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityAutoConfigurationTest.TestController.class)
@ContextConfiguration(classes = SecurityAutoConfigurationTest.TestApp.class)
@TestPropertySource(properties = "mango.security.permit-paths=/v3/api-docs,/v3/api-docs/**")
@DisplayName("Authorization security auto configuration tests")
class SecurityAutoConfigurationTest {

    @Resource
    private MockMvc mockMvc;

    @Test
    @DisplayName("configured public path should be permitted")
    void configuredPublicPathShouldBePermitted() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string("docs"));
    }

    @Test
    @DisplayName("unconfigured path should require authentication")
    void unconfiguredPathShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/authorization/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("verified internal request should be permitted")
    void verifiedInternalRequestShouldBePermitted() throws Exception {
        mockMvc.perform(get("/authorization/roles")
                        .requestAttr(InternalCallAttributes.VERIFIED, Boolean.TRUE))
                .andExpect(status().isOk())
                .andExpect(content().string("roles"));
    }

    @Test
    @DisplayName("unverified internal header should not bypass authentication")
    void unverifiedInternalHeaderShouldNotBypassAuthentication() throws Exception {
        mockMvc.perform(get("/authorization/roles")
                        .header("X-Internal-Call", "true"))
                .andExpect(status().isUnauthorized());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityAutoConfiguration.class, TestController.class})
    static class TestApp {
    }

    @RestController
    static class TestController {

        @GetMapping("/v3/api-docs")
        String docs() {
            return "docs";
        }

        @GetMapping("/authorization/roles")
        String roles() {
            return "roles";
        }
    }
}
