package io.mango.org.starter.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 组织初始化资源分层合同测试。
 */
class OrgResourceLayerContractTest {

    @Test
    void requiredAndDemoResourcesAreRegisteredSeparately() throws IOException {
        String required = resource("META-INF/mango/resources/org-required-bootstrap.yml");
        String demo = resource("META-INF/mango/demo/org-demo-structure.yml");

        assertThat(required).contains("ORG_UNIT:", "ORG_POST:", "MANGO_GROUP", "DEPT_MANAGER");
        assertThat(demo)
                .contains("ORG_UNIT:", "MANGO_COMPANY_A", "MANGO_COMPANY_B")
                .doesNotContain("ORG_POST:", "GREEN_MANGO_ROOT", "GREEN_COMPANY_C", "GREEN_COMPANY_D")
                .doesNotContain("tenantId: { type: LONG, value: 2 }")
                .doesNotContain("orgType: { type: INT, value: 4 }");
        assertThat(count(demo, "orgType: { type: INT, value: 1 }")).isZero();
        assertThat(count(demo, "orgType: { type: INT, value: 2 }")).isEqualTo(2);
        assertThat(count(demo, "orgType: { type: INT, value: 3 }")).isEqualTo(4);
        assertThat(count(demo, "sync-mode: INIT_ONLY")).isEqualTo(6);
        assertThat(required).doesNotContain("GREEN_MANGO_ROOT", "MANGO_COMPANY_A");
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int count(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
