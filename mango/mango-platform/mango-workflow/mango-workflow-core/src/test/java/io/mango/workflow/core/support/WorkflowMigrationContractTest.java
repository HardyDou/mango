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
    private static final String AUDIT_MIGRATION =
            "db/migration/workflow/V2__add_workflow_audit_columns.sql";
    private static final String PARTICIPATION_MIGRATION =
            "db/migration/workflow/V3__workflow_participation_auto_assignment.sql";
    private static final String CHECKSUM_CALLBACK =
            "db/migration/workflow/beforeValidate__workflow_v1_checksum_compatibility.sql";
    private static final Pattern DATA_MUTATION = Pattern.compile(
            "(?im)^\\s*(insert|update|delete|replace|set|prepare|execute|deallocate)\\b");
    private static final Pattern INTEGER_DISPLAY_WIDTH = Pattern.compile(
            "(?i)\\b(tinyint|smallint|mediumint|int|integer|bigint)\\s*\\(\\d+\\)");

    @Test
    void v1_containsFinalSchemaAndNoDataMutation() throws IOException {
        String sql = resourceText(MIGRATION);

        assertThat(DATA_MUTATION.matcher(sql).find()).isFalse();
        assertThat(sql)
                .contains("`tenant_id` varchar(64) NOT NULL DEFAULT '1'")
                .contains("`domain_code` varchar(64) NOT NULL DEFAULT 'COMMON'")
                .contains("`start_entry_visible` tinyint NOT NULL DEFAULT '1'")
                .contains("DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_bin")
                .contains("`claim_status` varchar(32)")
                .contains("`candidate_users` varchar(1000)")
                .contains("`candidate_groups` varchar(1000)")
                .doesNotContain("DEFAULT CHARSET=utf8 COLLATE utf8_bin")
                .doesNotContain("utf8mb3")
                .doesNotContain("common.schema.version")
                .doesNotContain("next.dbid")
                .doesNotContain("schema.history");
        assertThat(INTEGER_DISPLAY_WIDTH.matcher(sql).find()).isFalse();
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

    @Test
    void v2_conditionallyAddsEveryAuditColumnMissingFromMaven_1_0_20() throws IOException {
        String sql = resourceText(AUDIT_MIGRATION);

        assertThat(sql)
                .contains("information_schema.columns")
                .contains("PREPARE workflow_audit_column_stmt")
                .contains("`workflow_task_record` ADD COLUMN `created_by`")
                .contains("`workflow_copied_task` ADD COLUMN `created_by`")
                .contains("`workflow_business_apply_current_task` ADD COLUMN `created_by`")
                .contains("`workflow_business_apply_current_task` ADD COLUMN `updated_by`")
                .contains("`workflow_business_apply_status_log` ADD COLUMN `created_by`")
                .contains("`workflow_business_apply_status_log` ADD COLUMN `updated_by`")
                .contains("`workflow_business_apply_status_log` ADD COLUMN `updated_at`");
    }

    @Test
    void checksumCallback_repairsKnownPublishedV1Checksums() throws IOException {
        String sql = resourceText(CHECKSUM_CALLBACK);

        assertThat(sql)
                .contains("table_name = '${flyway:table}'")
                .contains("`checksum` = 1010539203")
                .contains("`checksum` IN (-840523381, -1500222187)")
                .contains("`version` = ''1''")
                .contains("`script` = ''V1__init_workflow.sql''")
                .contains("`success` = 1")
                .doesNotContain("validateOnMigrate");
    }

    @Test
    void v3_containsTenantScopedParticipationAndRoundRobinStateContracts() throws IOException {
        String sql = resourceText(PARTICIPATION_MIGRATION);

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS `workflow_process_participant`")
                .contains("`tenant_id` varchar(64) NOT NULL")
                .contains("`user_id` bigint NOT NULL")
                .contains("`participant_type` varchar(32) NOT NULL")
                .contains("UNIQUE KEY `uk_workflow_participant_instance_user_type`")
                .contains("CREATE TABLE IF NOT EXISTS `workflow_auto_assignment_state`")
                .contains("UNIQUE KEY `uk_workflow_auto_assignment_node`")
                .contains("ON DUPLICATE KEY UPDATE `id` = `id`")
                .doesNotContain("HAVING NOT EXISTS")
                .doesNotContain("username_snapshot` varchar(128) NOT NULL");
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
