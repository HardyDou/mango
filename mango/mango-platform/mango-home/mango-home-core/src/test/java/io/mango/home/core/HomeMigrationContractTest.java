package io.mango.home.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** 首页模块最终数据库基线合同测试。 */
class HomeMigrationContractTest {

    private static final String MIGRATION = "db/migration/home/V1__init_home.sql";
    private static final Pattern DATA_MUTATION = Pattern.compile(
            "(?im)^\\s*(insert|update|delete|replace|set|prepare|execute|deallocate)\\b");

    @Test
    void v1ContainsAllFinalTablesAndNoDataMutation() throws IOException {
        String sql = resourceText(MIGRATION);

        assertThat(DATA_MUTATION.matcher(sql).find()).isFalse();
        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS `sys_user_home_page`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_user_home_preference`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_home_template`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_home_template_version`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_home_template_authorization`")
                .contains("`default_home_ref` varchar(128)");
    }

    @Test
    void v1EntityTablesContainCanonicalTenantAndAuditColumns() throws IOException {
        String sql = resourceText(MIGRATION);
        List<String> entityTables = List.of(
                "sys_user_home_page",
                "sys_user_home_preference",
                "sys_home_template",
                "sys_home_template_version",
                "sys_home_template_authorization");

        for (String tableName : entityTables) {
            assertThat(tableDefinition(sql, tableName))
                    .as("TenantEntity columns of %s", tableName)
                    .contains("`tenant_id` varchar(64)")
                    .contains("`org_id` bigint")
                    .contains("`created_by` bigint")
                    .contains("`created_at` datetime")
                    .contains("`updated_by` bigint")
                    .contains("`updated_at` datetime");
        }
    }

    private String tableDefinition(String sql, String tableName) {
        Pattern tablePattern = Pattern.compile(
                "(?is)CREATE TABLE IF NOT EXISTS\\s+`"
                        + Pattern.quote(tableName)
                        + "`\\s*\\((.*?)\\)\\s*ENGINE=");
        var matcher = tablePattern.matcher(sql);
        assertThat(matcher.find()).as("table %s exists in %s", tableName, MIGRATION).isTrue();
        return matcher.group(1);
    }

    private String resourceText(String path) throws IOException {
        try (var input = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
