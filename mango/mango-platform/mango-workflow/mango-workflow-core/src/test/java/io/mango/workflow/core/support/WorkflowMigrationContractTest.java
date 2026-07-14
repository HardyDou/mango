package io.mango.workflow.core.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowMigrationContractTest {

    private static final String MIGRATION = "db/migration/workflow/V1__init_workflow.sql";
    private static final Pattern DATA_MUTATION = Pattern.compile(
            "(?im)^\\s*(insert|update|delete|replace|set|prepare|execute|deallocate)\\b");

    @Test
    void v1_containsFinalSchemaAndNoDataMutation() throws IOException {
        String sql = resourceText(MIGRATION);

        assertThat(DATA_MUTATION.matcher(sql).find()).isFalse();
        assertThat(sql)
                .contains("`tenant_id` varchar(64) NOT NULL DEFAULT '1'")
                .contains("`domain_code` varchar(64) NOT NULL DEFAULT 'COMMON'")
                .contains("`start_entry_visible` tinyint(1) NOT NULL DEFAULT '1'")
                .contains("`claim_status` varchar(32)")
                .contains("`candidate_users` varchar(1000)")
                .contains("`candidate_groups` varchar(1000)")
                .doesNotContain("common.schema.version")
                .doesNotContain("next.dbid")
                .doesNotContain("schema.history");
    }

    @Test
    void v1_workflowEntityTablesContainCanonicalTenantAndAuditColumns() throws IOException {
        String sql = resourceText(MIGRATION);
        List<String> workflowEntityTables = List.of(
                "workflow_category",
                "workflow_definition",
                "workflow_template_category",
                "workflow_template",
                "workflow_node_definition",
                "workflow_definition_version",
                "workflow_form_instance",
                "workflow_task_record",
                "workflow_copied_task",
                "workflow_business_apply",
                "workflow_business_apply_current_task",
                "workflow_business_apply_status_log");

        for (String tableName : workflowEntityTables) {
            assertThat(tableDefinition(sql, tableName))
                    .as("WorkflowBaseEntity columns of %s", tableName)
                    .contains("`tenant_id` varchar(64)")
                    .contains("`org_id` bigint")
                    .contains("`created_by` bigint DEFAULT NULL")
                    .contains("`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP")
                    .contains("`updated_by` bigint DEFAULT NULL")
                    .contains("`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
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
