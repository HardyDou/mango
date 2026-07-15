package io.mango.authorization.starter.resource;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Aligns resource-handler integration fixtures with the canonical authorization entity fields.
 */
final class AuthorizationStarterTestSchema {

    private AuthorizationStarterTestSchema() {
    }

    static void ensureCanonicalColumns(JdbcTemplate jdbcTemplate) {
        List<String> tables = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where lower(table_schema) = 'public'
                  and (lower(table_name) like 'authorization_%' or lower(table_name) like 'frontend_%')
                """, String.class);
        for (String table : tables) {
            addColumn(jdbcTemplate, table, "tenant_id varchar(64) default 'default'");
            addColumn(jdbcTemplate, table, "org_id bigint");
            addColumn(jdbcTemplate, table, "created_by bigint");
            addColumn(jdbcTemplate, table, "created_at timestamp default current_timestamp");
            addColumn(jdbcTemplate, table, "updated_by bigint");
            addColumn(jdbcTemplate, table, "updated_at timestamp default current_timestamp");
        }
    }

    private static void addColumn(JdbcTemplate jdbcTemplate, String table, String columnDefinition) {
        jdbcTemplate.execute("alter table " + table + " add column if not exists " + columnDefinition);
    }
}
