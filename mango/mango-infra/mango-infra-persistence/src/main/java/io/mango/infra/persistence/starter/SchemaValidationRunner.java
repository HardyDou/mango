package io.mango.infra.persistence.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 数据库结构校验执行器。
 */
public class SchemaValidationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaValidationRunner.class);
    private static final Set<String> SYSTEM_SCHEMAS = Set.of(
            "information_schema", "mysql", "performance_schema", "sys", "pg_catalog");

    private final DataSource dataSource;
    private final PersistenceProperties.SchemaValidation properties;

    public SchemaValidationRunner(DataSource dataSource, PersistenceProperties.SchemaValidation properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    public void run() throws SQLException {
        List<String> violations = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();
            try (ResultSet tables = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableSchema = tables.getString("TABLE_SCHEM");
                    String tableName = tables.getString("TABLE_NAME");
                    validateTable(metaData, catalog, tableSchema, tableName, violations);
                }
            }
        }
        if (violations.isEmpty()) {
            LOGGER.info("数据库结构校验通过");
            return;
        }
        String message = "数据库结构校验发现 " + violations.size() + " 个问题: " + String.join("; ", violations);
        if (properties.isFailFast()) {
            throw new IllegalStateException(message);
        }
        LOGGER.warn(message);
    }

    private void validateTable(DatabaseMetaData metaData,
                               String catalog,
                               String schema,
                               String tableName,
                               List<String> violations) throws SQLException {
        if (shouldSkip(schema, tableName)) {
            return;
        }
        Set<String> columns = new HashSet<>();
        try (ResultSet resultSet = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (resultSet.next()) {
                columns.add(normalize(resultSet.getString("COLUMN_NAME")));
            }
        }
        for (String requiredColumn : properties.getRequiredColumns()) {
            String column = normalize(requiredColumn);
            if (!columns.contains(column)) {
                violations.add(tableName + " 缺少字段 " + column);
            }
        }
    }

    private boolean shouldSkip(String schema, String tableName) {
        String normalizedSchema = normalize(schema);
        if (normalizedSchema != null && SYSTEM_SCHEMAS.contains(normalizedSchema)) {
            return true;
        }
        String normalizedTable = normalize(tableName);
        if (normalizedTable == null) {
            return true;
        }
        for (String excludedTable : properties.getExcludedTables()) {
            if (normalizedTable.equals(normalize(excludedTable))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
