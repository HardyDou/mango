package io.mango.workflow.core.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private String resourceText(String path) throws IOException {
        try (var input = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
