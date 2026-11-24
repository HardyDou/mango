package io.mango.authorization.starter.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationDemoResourceContractTest {

    @Test
    void demoBindingsOnlyCoverTheSingleDefaultTenantAndUseSyncOnce() throws IOException {
        String demo = resource("META-INF/mango/demo/authorization-demo-role.yml");

        assertThat(demo)
                .doesNotContain("AUTH_ROLE:")
                .contains("authorization.subject-role.tenant-1.admin")
                .doesNotContain("tenant-2", "tenant-3", "tenant-4", "value: 租户管理员");
        assertThat(count(demo, "sync-mode: SYNC_ONCE")).isEqualTo(1);
        assertThat(demo).doesNotContain("sync-mode: INIT_ONLY");
    }

    @Test
    void bootstrapResourcesDeclareBuiltInAdminRoleForFreshDatabases() throws IOException {
        String bootstrap = resource("META-INF/mango/resources/authorization-common-bootstrap.yml");
        String demo = resource("META-INF/mango/demo/authorization-demo-role.yml");

        assertThat(bootstrap)
                .contains("authorization.role.tenant-1.admin")
                .contains("value: ROLE_ADMIN")
                .contains("value: 平台超级管理员");
        assertThat(demo).doesNotContain("value: ROLE_ADMIN");
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
