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
        boolean hasStandardId = false;
        boolean standardIdIsBigint = false;
        boolean standardIdAutoIncrement = false;
        try (ResultSet resultSet = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (resultSet.next()) {
                String columnName = normalize(resultSet.getString("COLUMN_NAME"));
                columns.add(columnName);
                if ("id".equals(columnName)) {
                    hasStandardId = true;
                    standardIdIsBigint = isBigint(resultSet);
                    standardIdAutoIncrement = isAutoIncrement(resultSet);
                }
            }
        }
        if (!hasStandardId) {
            violations.add(tableName + " 必须使用标准主键字段 id");
        } else {
            if (!standardIdIsBigint) {
                violations.add(tableName + " 标准主键 id 必须为 BIGINT");
            }
            if (standardIdAutoIncrement) {
                violations.add(tableName + " 标准主键 id 必须使用雪花算法，不允许 AUTO_INCREMENT");
            }
        }
        if (hasStandardId && !hasIdPrimaryKey(metaData, catalog, schema, tableName)) {
            violations.add(tableName + " 必须以 id 作为主键");
        }
        for (String requiredColumn : properties.getRequiredColumns()) {
            String column = normalize(requiredColumn);
            if (!columns.contains(column)) {
                violations.add(tableName + " 缺少字段 " + column);
            }
        }
    }

    private boolean isBigint(ResultSet column) throws SQLException {
        String typeName = normalize(column.getString("TYPE_NAME"));
        if (typeName != null && (typeName.contains("bigint") || typeName.contains("int8"))) {
            return true;
        }
        return column.getInt("DATA_TYPE") == java.sql.Types.BIGINT;
    }

    private boolean isAutoIncrement(ResultSet column) throws SQLException {
        try {
            return "YES".equalsIgnoreCase(column.getString("IS_AUTOINCREMENT"));
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean hasIdPrimaryKey(DatabaseMetaData metaData, String catalog, String schema, String tableName)
            throws SQLException {
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (primaryKeys.next()) {
                if ("id".equals(normalize(primaryKeys.getString("COLUMN_NAME")))) {
                    return true;
                }
            }
        }
        return false;
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
            String normalizedExcludedTable = normalize(excludedTable);
            if (matchesExcludedTable(normalizedTable, normalizedExcludedTable)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesExcludedTable(String tableName, String excludedTable) {
        if (excludedTable == null) {
            return false;
        }
        if (tableName.equals(excludedTable)) {
            return true;
        }
        return excludedTable.endsWith("*") && tableName.startsWith(excludedTable.substring(0, excludedTable.length() - 1));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
