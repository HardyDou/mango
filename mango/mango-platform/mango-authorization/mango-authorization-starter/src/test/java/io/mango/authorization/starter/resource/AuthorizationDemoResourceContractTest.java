package io.mango.authorization.starter.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationDemoResourceContractTest {

    @Test
    void demoRolesOnlyCoverTheSingleDefaultTenant() throws IOException {
        String demo = resource("META-INF/mango/demo/authorization-demo-role.yml");

        assertThat(demo)
                .contains("authorization.role.tenant-1.admin")
                .contains("value: 平台超级管理员")
                .doesNotContain("tenant-2", "tenant-3", "tenant-4", "value: 租户管理员");
        assertThat(count(demo, "sync-mode: INIT_ONLY")).isEqualTo(2);
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int count(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
