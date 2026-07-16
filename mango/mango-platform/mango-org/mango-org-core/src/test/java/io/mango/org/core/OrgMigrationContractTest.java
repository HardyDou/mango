package io.mango.org.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 组织模块数据库基线合同测试。
 */
class OrgMigrationContractTest {

    private static final String MIGRATION = "db/migration/org/V1__init_org.sql";

    @Test
    void v1ContainsOnlyCanonicalSchemaDefinition() throws IOException {
        String sql = resource(MIGRATION);
        String normalized = sql.toUpperCase();

        assertThat(normalized).contains("CREATE TABLE IF NOT EXISTS `SYS_ORG`");
        assertThat(normalized).contains("CREATE TABLE IF NOT EXISTS `ORG_POST`");
        assertThat(normalized).contains("`TENANT_ID` VARCHAR(64)");
        assertThat(normalized).contains("`ORG_ID` BIGINT");
        assertThat(normalized).contains("`CREATED_BY` BIGINT");
        assertThat(normalized).contains("`CREATED_AT` DATETIME");
        assertThat(normalized).contains("`UPDATED_BY` BIGINT");
        assertThat(normalized).contains("`UPDATED_AT` DATETIME");
        assertThat(normalized).doesNotContain("INSERT INTO");
        assertThat(normalized).doesNotContain("LOCK TABLES");
        assertThat(normalized).doesNotContain("CREATE_TIME");
        assertThat(normalized).doesNotContain("UPDATE_TIME");
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
