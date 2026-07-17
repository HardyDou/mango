package io.mango.template.core;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateMigrationContractTest {

    @Test
    void newDatabaseUsesOneCanonicalDdlOnlyV1() throws Exception {
        URI migrationUri = getClass().getClassLoader().getResource("db/migration/template").toURI();
        List<String> migrations;
        try (var paths = Files.list(Path.of(migrationUri))) {
            migrations = paths.map(path -> path.getFileName().toString()).sorted().toList();
        }

        assertThat(migrations).containsExactly("V1__init_template.sql");
        String sql = Files.readString(Path.of(getClass().getClassLoader()
                .getResource("db/migration/template/V1__init_template.sql").toURI()));
        String normalized = sql.toLowerCase(Locale.ROOT);

        assertThat(normalized)
                .doesNotContain("alter table", "insert into", "delete from")
                .contains("create table if not exists `template`")
                .contains("`tenant_id` varchar(64) not null")
                .contains("`org_id` bigint")
                .contains("`domain_code` varchar(64) not null")
                .contains("`draft_variable_schema` json")
                .contains("`created_at` datetime")
                .contains("`updated_at` datetime")
                .doesNotContain("`created_time`", "`updated_time`");
        assertThat(normalized).doesNotMatch("(?s).*;\\s*update\\s+.*");
    }
}
