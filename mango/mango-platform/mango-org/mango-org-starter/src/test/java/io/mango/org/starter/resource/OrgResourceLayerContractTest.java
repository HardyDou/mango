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
        assertThat(demo).contains("ORG_UNIT:", "ORG_POST:", "COMPANY_A_ROOT", "TECH_RD");
        assertThat(required).doesNotContain("COMPANY_A_ROOT", "TECH_RD");
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
