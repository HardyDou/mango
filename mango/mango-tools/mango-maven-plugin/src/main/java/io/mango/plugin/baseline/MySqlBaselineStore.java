package io.mango.plugin.baseline;

import org.apache.maven.plugin.MojoExecutionException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

final class MySqlBaselineStore {

    private static final String MYSQL_PRODUCT_NAME = "mysql";
    private static final String RESOURCE_REGISTRY_TABLE = "resource_registry";
    private static final String SAFE_CHARACTER_SET_PATTERN = "[a-zA-Z0-9_]+";
    private static final String SAFE_DATABASE_NAME_PATTERN = "[a-z][a-z0-9_]{0,63}";
    private static final int SQL_BUFFER_CAPACITY = 64_000;
    private static final int SHOW_CREATE_FALLBACK_COLUMN = 3;
    private static final int DATA_TYPE_COLUMN = 3;
    private static final int CHARACTER_SET_COLUMN = 4;
    private static final Pattern AUTO_INCREMENT = Pattern.compile("(?i)\\s+AUTO_INCREMENT=\\d+");
    private static final Pattern DEFINER = Pattern.compile(
            "(?i)DEFINER\\s*=\\s*(?:`[^`]*`|[^@\\s]+)@(?:`[^`]*`|[^\\s]+)\\s*");
    private static final Set<String> RUNTIME_AUDIT_TIMESTAMP_COLUMNS = Set.of(
            "created_at", "updated_at", "published_at", "last_sync_time",
            "create_time", "update_time", "created_time", "updated_time");
    private static final Set<String> AUDIT_TEMPORAL_TYPES = Set.of("date", "datetime", "timestamp");
    private static final String CANONICAL_AUDIT_DATE = "2000-01-01";
    private static final String CANONICAL_AUDIT_DATE_TIME = "2000-01-01 00:00:00";
    private static final int INSERT_BATCH_SIZE = 250;

    private final MySqlJdbcUrl jdbcUrl;
    private final String username;
    private final String password;

    MySqlBaselineStore(String jdbcUrl, String username, String password)
            throws MojoExecutionException {
        this.jdbcUrl = MySqlJdbcUrl.parse(jdbcUrl);
        this.username = username;
        this.password = password;
    }

    DatabaseIdentity databaseIdentity(String database) throws MojoExecutionException {
        try (Connection connection = connect(database)) {
            DatabaseMetaData metadata = connection.getMetaData();
            String product = metadata.getDatabaseProductName();
            if (!product.toLowerCase(Locale.ROOT).contains(MYSQL_PRODUCT_NAME)) {
                throw new MojoExecutionException(
                        "MANGO-BASELINE-014 only MySQL is supported, actual product=" + product);
            }
            return new DatabaseIdentity(product, metadata.getDatabaseProductVersion());
        } catch (SQLException exception) {
            throw databaseFailure("read database identity", exception);
        }
    }

    void validateSchemaDefaults(String adminDatabase, MySqlSchemaDefaults defaults)
            throws MojoExecutionException {
        try (Connection connection = connect(adminDatabase);
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT COUNT(*)
                        FROM information_schema.COLLATIONS
                        WHERE CHARACTER_SET_NAME = ? AND COLLATION_NAME = ?
                        """)) {
            statement.setString(1, defaults.characterSet());
            statement.setString(2, defaults.collation());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) != 1) {
                    throw new MojoExecutionException(
                            "MANGO-BASELINE-043 unsupported MySQL character set and collation"
                                    + "; characterSet=" + defaults.characterSet()
                                    + ", collation=" + defaults.collation());
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure("validate target character set and collation", exception);
        }
    }

    void createDatabase(
            String adminDatabase,
            String database,
            MySqlSchemaDefaults defaults) throws MojoExecutionException {
        validateDatabaseName(database);
        try (Connection connection = connect(adminDatabase);
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + quote(database)
                    + " CHARACTER SET " + defaults.characterSet()
                    + " COLLATE " + defaults.collation());
        } catch (SQLException exception) {
            throw databaseFailure("create temporary database " + database, exception);
        }
    }

    void dropDatabase(String adminDatabase, String database) throws MojoExecutionException {
        validateDatabaseName(database);
        try (Connection connection = connect(adminDatabase);
                Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + quote(database));
        } catch (SQLException exception) {
            throw databaseFailure("drop temporary database " + database, exception);
        }
    }

    String databaseUrl(String database) {
        return jdbcUrl.database(database);
    }

    void preparePortableResourceBaseline(String database) throws MojoExecutionException {
        try (Connection connection = connect(database);
                Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            deleteIfPresent(connection, database, "resource_module_receipt");
            deleteIfPresent(connection, database, "resource_sync_log");
            deleteIfPresent(connection, database, "resource_change_log");
            normalizeResourceRegistryIds(connection, database);
            for (String table : List.of(
                    "mango_runtime_instance",
                    "mango_bootstrap_step_execution",
                    "mango_bootstrap_execution",
                    "mango_bootstrap_control")) {
                statement.execute("DROP TABLE IF EXISTS " + quote(table));
            }
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
        } catch (SQLException exception) {
            throw databaseFailure("prepare portable Resource baseline " + database, exception);
        }
    }

    void canonicalizeRuntimeAuditTimestamps(String database) throws MojoExecutionException {
        try (Connection connection = connect(database)) {
            Map<String, List<AuditTemporalColumn>> columnsByTable = auditTemporalColumns(
                    connection, database);
            for (Map.Entry<String, List<AuditTemporalColumn>> entry : columnsByTable.entrySet()) {
                List<AuditTemporalColumn> columns = entry.getValue();
                String assignments = columns.stream()
                        .map(column -> quote(column.name()) + " = CASE WHEN "
                                + quote(column.name()) + " IS NULL THEN NULL ELSE ? END")
                        .reduce((left, right) -> left + ", " + right)
                        .orElseThrow();
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE " + quote(entry.getKey()) + " SET " + assignments)) {
                    for (int index = 0; index < columns.size(); index++) {
                        statement.setString(index + 1, canonicalAuditValue(columns.get(index).dataType()));
                    }
                    statement.executeUpdate();
                }
            }
        } catch (SQLException exception) {
            throw databaseFailure("canonicalize runtime audit timestamps in " + database, exception);
        }
    }

    private static Map<String, List<AuditTemporalColumn>> auditTemporalColumns(
            Connection connection,
            String database) throws SQLException {
        Map<String, List<AuditTemporalColumn>> columnsByTable = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ?
                ORDER BY TABLE_NAME, ORDINAL_POSITION
                """)) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String columnName = resultSet.getString(2);
                    String dataType = resultSet.getString(3).toLowerCase(Locale.ROOT);
                    if (RUNTIME_AUDIT_TIMESTAMP_COLUMNS.contains(columnName.toLowerCase(Locale.ROOT))
                            && AUDIT_TEMPORAL_TYPES.contains(dataType)) {
                        columnsByTable.computeIfAbsent(resultSet.getString(1), ignored -> new ArrayList<>())
                                .add(new AuditTemporalColumn(columnName, dataType));
                    }
                }
            }
        }
        return columnsByTable;
    }

    private static String canonicalAuditValue(String dataType) {
        return "date".equals(dataType) ? CANONICAL_AUDIT_DATE : CANONICAL_AUDIT_DATE_TIME;
    }

    private static void deleteIfPresent(Connection connection, String database, String table)
            throws SQLException {
        if (tableExists(connection, database, table)) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM " + quote(table));
            }
        }
    }

    private static void normalizeResourceRegistryIds(Connection connection, String database)
            throws SQLException {
        if (!tableExists(connection, database, RESOURCE_REGISTRY_TABLE)) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT id FROM resource_registry ORDER BY resource_id")) {
            while (rows.next()) {
                ids.add(rows.getLong(1));
            }
        }
        try (PreparedStatement negative = connection.prepareStatement(
                    "UPDATE resource_registry SET id = ? WHERE id = ?");
                PreparedStatement positive = connection.prepareStatement(
                    "UPDATE resource_registry SET id = ? WHERE id = ?")) {
            for (int index = 0; index < ids.size(); index++) {
                negative.setLong(1, -(index + 1L));
                negative.setLong(2, ids.get(index));
                negative.addBatch();
            }
            negative.executeBatch();
            for (int index = 0; index < ids.size(); index++) {
                positive.setLong(1, index + 1L);
                positive.setLong(2, -(index + 1L));
                positive.addBatch();
            }
            positive.executeBatch();
        }
    }

    private static boolean tableExists(Connection connection, String database, String table)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                """)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    String dumpModule(
            String database,
            String module,
            Set<String> groupModules,
            BaselineObjectOwnership ownership) throws MojoExecutionException {
        try (Connection connection = connect(database)) {
            DatabaseObjects objects = inspectObjects(connection, database);
            validateOwnership(objects, groupModules, ownership);

            StringBuilder sql = new StringBuilder(SQL_BUFFER_CAPACITY);
            sql.append("-- mango:baseline-idempotent\n")
                    .append("-- generated by mango:baseline-generate; do not edit\n")
                    .append("SET FOREIGN_KEY_CHECKS=0;\n");

            List<String> moduleTables = objects.tables().entrySet().stream()
                    .filter(entry -> module.equals(ownership.tableOwner(entry.getKey())))
                    .map(Map.Entry::getValue)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            for (String table : moduleTables) {
                sql.append(idempotentCreateTable(showCreateTable(connection, table))).append(";\n");
                appendTableData(connection, database, table, sql);
            }

            List<String> moduleViews = objects.views().entrySet().stream()
                    .filter(entry -> module.equals(ownership.viewOwner(entry.getKey())))
                    .map(Map.Entry::getValue)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            for (String view : moduleViews) {
                sql.append(idempotentCreateView(
                        portableDefinition(showCreateView(connection, view), database))).append(";\n");
            }

            for (TriggerDefinition trigger : triggers(connection, database)) {
                String owner = ownership.tableOwner(trigger.tableName());
                if (module.equals(owner)) {
                    sql.append("DROP TRIGGER IF EXISTS ").append(quote(trigger.name())).append(";\n");
                    sql.append(portableDefinition(trigger.createStatement(), database)).append(";\n");
                }
            }
            sql.append("SET FOREIGN_KEY_CHECKS=1;\n");
            return sql.toString();
        } catch (SQLException exception) {
            throw databaseFailure("dump baseline for module " + module, exception);
        }
    }

    SchemaSnapshot snapshot(
            String database,
            Set<String> groupModules,
            BaselineObjectOwnership ownership) throws MojoExecutionException {
        return snapshot(database, groupModules, ownership, Set.of());
    }

    SchemaSnapshot determinismSnapshot(
            String database,
            Set<String> groupModules,
            BaselineObjectOwnership ownership) throws MojoExecutionException {
        return snapshot(database, groupModules, ownership, RUNTIME_AUDIT_TIMESTAMP_COLUMNS);
    }

    private SchemaSnapshot snapshot(
            String database,
            Set<String> groupModules,
            BaselineObjectOwnership ownership,
            Set<String> ignoredDataColumns) throws MojoExecutionException {
        try (Connection connection = connect(database)) {
            DatabaseObjects objects = inspectObjects(connection, database);
            validateOwnership(objects, groupModules, ownership);
            Map<String, TableSnapshot> tables = new TreeMap<>();
            Map<String, String> definitions = new TreeMap<>();
            Map<String, List<List<String>>> data = new TreeMap<>();
            for (Map.Entry<String, String> table : objects.tables().entrySet()) {
                if (!groupModules.contains(ownership.tableOwner(table.getKey()))) {
                    continue;
                }
                tables.put(table.getKey(), tableSnapshot(connection, database, table.getValue()));
                data.put(table.getKey(), readHexRows(
                        connection, database, table.getValue(), ignoredDataColumns));
            }
            for (Map.Entry<String, String> view : objects.views().entrySet()) {
                if (!groupModules.contains(ownership.viewOwner(view.getKey()))) {
                    continue;
                }
                definitions.put("view:" + view.getKey(),
                        normalizeDefinition(showCreateView(connection, view.getValue()), database));
            }
            for (TriggerDefinition trigger : triggers(connection, database)) {
                if (groupModules.contains(ownership.tableOwner(trigger.tableName()))) {
                    definitions.put("trigger:" + trigger.name().toLowerCase(Locale.ROOT),
                            normalizeDefinition(trigger.createStatement(), database));
                }
            }
            assertNoUnsupportedObjects(connection, database);
            return new SchemaSnapshot(
                    Map.copyOf(tables), Map.copyOf(definitions), Map.copyOf(data));
        } catch (SQLException exception) {
            throw databaseFailure("read schema snapshot from " + database, exception);
        }
    }

    private Connection connect(String database) throws SQLException {
        return DriverManager.getConnection(jdbcUrl.database(database), username, password);
    }

    private static DatabaseObjects inspectObjects(Connection connection, String database)
            throws SQLException {
        Map<String, String> tables = new TreeMap<>();
        Map<String, String> views = new TreeMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TABLE_NAME, TABLE_TYPE
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ?
                ORDER BY TABLE_NAME
                """)) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String actualName = resultSet.getString(1);
                    String normalized = actualName.toLowerCase(Locale.ROOT);
                    if (isHistoryTable(normalized)) {
                        continue;
                    }
                    if ("VIEW".equalsIgnoreCase(resultSet.getString(2))) {
                        views.put(normalized, actualName);
                    } else {
                        tables.put(normalized, actualName);
                    }
                }
            }
        }
        return new DatabaseObjects(tables, views);
    }

    private static TableSnapshot tableSnapshot(
            Connection connection,
            String database,
            String table) throws SQLException {
        TableHeader header = tableHeader(connection, database, table);
        return new TableSnapshot(
                header.engine(),
                header.characterSet(),
                header.collation(),
                header.rowFormat(),
                header.createOptions(),
                header.comment(),
                tableColumns(connection, database, table),
                tableIndexes(connection, database, table),
                tableConstraints(connection, database, table));
    }

    private static TableHeader tableHeader(
            Connection connection,
            String database,
            String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT t.ENGINE,
                       cca.CHARACTER_SET_NAME,
                       t.TABLE_COLLATION,
                       t.ROW_FORMAT,
                       t.CREATE_OPTIONS,
                       t.TABLE_COMMENT
                FROM information_schema.TABLES t
                LEFT JOIN information_schema.COLLATION_CHARACTER_SET_APPLICABILITY cca
                  ON cca.COLLATION_NAME = t.TABLE_COLLATION
                WHERE t.TABLE_SCHEMA = ?
                  AND t.TABLE_NAME = ?
                  AND t.TABLE_TYPE = 'BASE TABLE'
                """)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("table metadata returned no row for " + table);
                }
                return new TableHeader(
                        normalizeIdentifier(resultSet.getString("ENGINE")),
                        normalizeIdentifier(resultSet.getString("CHARACTER_SET_NAME")),
                        normalizeIdentifier(resultSet.getString("TABLE_COLLATION")),
                        normalizeIdentifier(resultSet.getString("ROW_FORMAT")),
                        normalizeOptions(resultSet.getString("CREATE_OPTIONS")),
                        resultSet.getString("TABLE_COMMENT"));
            }
        }
    }

    private static Map<String, ColumnSnapshot> tableColumns(
            Connection connection,
            String database,
            String table) throws SQLException {
        Map<String, ColumnSnapshot> columns = new TreeMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT ORDINAL_POSITION,
                       COLUMN_NAME,
                       COLUMN_TYPE,
                       IS_NULLABLE,
                       COLUMN_DEFAULT,
                       EXTRA,
                       GENERATION_EXPRESSION,
                       CHARACTER_SET_NAME,
                       COLLATION_NAME,
                       COLUMN_COMMENT
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString("COLUMN_NAME");
                    columns.put(name.toLowerCase(Locale.ROOT), new ColumnSnapshot(
                            resultSet.getInt("ORDINAL_POSITION"),
                            name,
                            normalizeIdentifier(resultSet.getString("COLUMN_TYPE")),
                            "YES".equalsIgnoreCase(resultSet.getString("IS_NULLABLE")),
                            resultSet.getString("COLUMN_DEFAULT"),
                            normalizeSqlFragment(resultSet.getString("EXTRA")),
                            normalizeSqlFragment(resultSet.getString("GENERATION_EXPRESSION")),
                            normalizeIdentifier(resultSet.getString("CHARACTER_SET_NAME")),
                            normalizeIdentifier(resultSet.getString("COLLATION_NAME")),
                            resultSet.getString("COLUMN_COMMENT")));
                }
            }
        }
        return Map.copyOf(columns);
    }

    private static Map<String, IndexSnapshot> tableIndexes(
            Connection connection,
            String database,
            String table) throws SQLException {
        Map<String, IndexHeader> headers = new TreeMap<>();
        Map<String, List<IndexColumnSnapshot>> parts = new TreeMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT INDEX_NAME,
                       NON_UNIQUE,
                       INDEX_TYPE,
                       COMMENT,
                       INDEX_COMMENT,
                       IS_VISIBLE,
                       SEQ_IN_INDEX,
                       COLUMN_NAME,
                       COLLATION,
                       SUB_PART,
                       NULLABLE,
                       EXPRESSION
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY INDEX_NAME, SEQ_IN_INDEX
                """)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString("INDEX_NAME");
                    String key = name.toLowerCase(Locale.ROOT);
                    headers.putIfAbsent(key, new IndexHeader(
                            !resultSet.getBoolean("NON_UNIQUE"),
                            normalizeIdentifier(resultSet.getString("INDEX_TYPE")),
                            resultSet.getString("COMMENT"),
                            resultSet.getString("INDEX_COMMENT"),
                            "YES".equalsIgnoreCase(resultSet.getString("IS_VISIBLE"))));
                    parts.computeIfAbsent(key, ignored -> new ArrayList<>()).add(
                            new IndexColumnSnapshot(
                                    resultSet.getInt("SEQ_IN_INDEX"),
                                    resultSet.getString("COLUMN_NAME"),
                                    normalizeIdentifier(resultSet.getString("COLLATION")),
                                    nullableLong(resultSet, "SUB_PART"),
                                    "YES".equalsIgnoreCase(resultSet.getString("NULLABLE")),
                                    normalizeSqlFragment(resultSet.getString("EXPRESSION"))));
                }
            }
        }
        Map<String, IndexSnapshot> indexes = new TreeMap<>();
        headers.forEach((name, header) -> indexes.put(name, new IndexSnapshot(
                header.unique(),
                header.type(),
                header.comment(),
                header.indexComment(),
                header.visible(),
                List.copyOf(parts.getOrDefault(name, List.of())))));
        return Map.copyOf(indexes);
    }

    private static Map<String, ConstraintSnapshot> tableConstraints(
            Connection connection,
            String database,
            String table) throws SQLException {
        Map<String, ConstraintHeader> headers = constraintHeaders(connection, database, table);
        Map<String, List<ConstraintColumnSnapshot>> columns =
                constraintColumns(connection, database, table);
        Map<String, ReferentialConstraintSnapshot> references =
                referentialConstraints(connection, database, table);
        Map<String, String> checkClauses = checkConstraints(connection, database, table);
        Map<String, ConstraintSnapshot> constraints = new TreeMap<>();
        headers.forEach((name, header) -> constraints.put(name, new ConstraintSnapshot(
                header.type(),
                header.enforced(),
                List.copyOf(columns.getOrDefault(name, List.of())),
                references.get(name),
                checkClauses.get(name))));
        return Map.copyOf(constraints);
    }

    private static Map<String, ConstraintHeader> constraintHeaders(
            Connection connection,
            String database,
            String table) throws SQLException {
        Map<String, ConstraintHeader> headers = new TreeMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, ENFORCED
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY CONSTRAINT_NAME
                """)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    headers.put(resultSet.getString("CONSTRAINT_NAME").toLowerCase(Locale.ROOT),
                            new ConstraintHeader(
                                    normalizeIdentifier(resultSet.getString("CONSTRAINT_TYPE")),
                                    "YES".equalsIgnoreCase(resultSet.getString("ENFORCED"))));
                }
            }
        }
        return headers;
    }

    private static Map<String, List<ConstraintColumnSnapshot>> constraintColumns(
            Connection connection,
            String database,
            String table) throws SQLException {
        Map<String, List<ConstraintColumnSnapshot>> columns = new TreeMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT CONSTRAINT_NAME,
                       ORDINAL_POSITION,
                       POSITION_IN_UNIQUE_CONSTRAINT,
                       COLUMN_NAME,
                       REFERENCED_TABLE_SCHEMA,
                       REFERENCED_TABLE_NAME,
                       REFERENCED_COLUMN_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE CONSTRAINT_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY CONSTRAINT_NAME, ORDINAL_POSITION
                """)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String key = resultSet.getString("CONSTRAINT_NAME").toLowerCase(Locale.ROOT);
                    columns.computeIfAbsent(key, ignored -> new ArrayList<>()).add(
                            new ConstraintColumnSnapshot(
                                    resultSet.getInt("ORDINAL_POSITION"),
                                    nullableLong(resultSet, "POSITION_IN_UNIQUE_CONSTRAINT"),
                                    resultSet.getString("COLUMN_NAME"),
                                    portableSchema(resultSet.getString("REFERENCED_TABLE_SCHEMA"), database),
                                    resultSet.getString("REFERENCED_TABLE_NAME"),
                                    resultSet.getString("REFERENCED_COLUMN_NAME")));
                }
            }
        }
        return columns;
    }

    private static Map<String, ReferentialConstraintSnapshot> referentialConstraints(
            Connection connection,
            String database,
            String table) throws SQLException {
        Map<String, ReferentialConstraintSnapshot> references = new TreeMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT CONSTRAINT_NAME,
                       UNIQUE_CONSTRAINT_SCHEMA,
                       UNIQUE_CONSTRAINT_NAME,
                       MATCH_OPTION,
                       UPDATE_RULE,
                       DELETE_RULE
                FROM information_schema.REFERENTIAL_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY CONSTRAINT_NAME
                """)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    references.put(resultSet.getString("CONSTRAINT_NAME").toLowerCase(Locale.ROOT),
                            new ReferentialConstraintSnapshot(
                                    portableSchema(resultSet.getString("UNIQUE_CONSTRAINT_SCHEMA"), database),
                                    resultSet.getString("UNIQUE_CONSTRAINT_NAME"),
                                    normalizeIdentifier(resultSet.getString("MATCH_OPTION")),
                                    normalizeIdentifier(resultSet.getString("UPDATE_RULE")),
                                    normalizeIdentifier(resultSet.getString("DELETE_RULE"))));
                }
            }
        }
        return references;
    }

    private static Map<String, String> checkConstraints(
            Connection connection,
            String database,
            String table) throws SQLException {
        Map<String, String> clauses = new TreeMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT cc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
                FROM information_schema.CHECK_CONSTRAINTS cc
                JOIN information_schema.TABLE_CONSTRAINTS tc
                  ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
                 AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                WHERE tc.CONSTRAINT_SCHEMA = ? AND tc.TABLE_NAME = ?
                ORDER BY cc.CONSTRAINT_NAME
                """)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    clauses.put(resultSet.getString(1).toLowerCase(Locale.ROOT),
                            normalizeSqlFragment(resultSet.getString(2)));
                }
            }
        }
        return clauses;
    }

    private static Long nullableLong(ResultSet resultSet, String columnLabel) throws SQLException {
        long value = resultSet.getLong(columnLabel);
        return resultSet.wasNull() ? null : value;
    }

    private static String portableSchema(String schema, String database) {
        return schema != null && schema.equalsIgnoreCase(database) ? "${schema}" : schema;
    }

    private static String normalizeIdentifier(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static String normalizeOptions(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        List<String> options = new ArrayList<>(List.of(value.trim().split("\\s+")));
        options.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join(" ", options).toLowerCase(Locale.ROOT);
    }

    private static String normalizeSqlFragment(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private static void validateOwnership(
            DatabaseObjects objects,
            Set<String> groupModules,
            BaselineObjectOwnership ownership) throws MojoExecutionException {
        Set<String> missing = new LinkedHashSet<>();
        for (String table : objects.tables().keySet()) {
            String owner = ownership.tableOwner(table);
            if (owner == null || !groupModules.contains(owner)) {
                missing.add("table:" + table);
            }
        }
        for (String view : objects.views().keySet()) {
            String owner = ownership.viewOwner(view);
            if (owner == null || !groupModules.contains(owner)) {
                missing.add("view:" + view);
            }
        }
        if (!missing.isEmpty()) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-015 database objects have no owner in this datasource group: "
                            + missing);
        }
    }

    private static String showCreateTable(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SHOW CREATE TABLE " + quote(table))) {
            if (!resultSet.next()) {
                throw new SQLException("SHOW CREATE TABLE returned no row for " + table);
            }
            return resultSet.getString(2);
        }
    }

    private static String showCreateView(Connection connection, String view) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SHOW CREATE VIEW " + quote(view))) {
            if (!resultSet.next()) {
                throw new SQLException("SHOW CREATE VIEW returned no row for " + view);
            }
            return resultSet.getString(2);
        }
    }

    private static List<TriggerDefinition> triggers(Connection connection, String database)
            throws SQLException {
        List<TriggerDefinition> triggers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT TRIGGER_NAME, EVENT_OBJECT_TABLE
                FROM information_schema.TRIGGERS
                WHERE TRIGGER_SCHEMA = ?
                ORDER BY TRIGGER_NAME
                """)) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString(1);
                    String table = resultSet.getString(2).toLowerCase(Locale.ROOT);
                    triggers.add(new TriggerDefinition(name, table, showCreateTrigger(connection, name)));
                }
            }
        }
        return triggers;
    }

    private static String showCreateTrigger(Connection connection, String trigger) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SHOW CREATE TRIGGER " + quote(trigger))) {
            if (!resultSet.next()) {
                throw new SQLException("SHOW CREATE TRIGGER returned no row for " + trigger);
            }
            ResultSetMetaData metadata = resultSet.getMetaData();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                if ("SQL Original Statement".equalsIgnoreCase(metadata.getColumnLabel(index))) {
                    return resultSet.getString(index);
                }
            }
            return resultSet.getString(Math.min(
                    SHOW_CREATE_FALLBACK_COLUMN, metadata.getColumnCount()));
        }
    }

    private static void appendTableData(
            Connection connection,
            String database,
            String table,
            StringBuilder sql) throws SQLException {
        List<ColumnSpec> columns = insertableColumns(connection, database, table);
        if (columns.isEmpty()) {
            return;
        }
        List<List<String>> rows = readHexRows(connection, database, table, columns);
        for (int offset = 0; offset < rows.size(); offset += INSERT_BATCH_SIZE) {
            int end = Math.min(offset + INSERT_BATCH_SIZE, rows.size());
            sql.append("INSERT IGNORE INTO ").append(quote(table)).append(" (")
                    .append(columns.stream().map(ColumnSpec::name).map(MySqlBaselineStore::quote)
                            .reduce((a, b) -> a + ", " + b)
                            .orElseThrow())
                    .append(") VALUES\n");
            for (int rowIndex = offset; rowIndex < end; rowIndex++) {
                if (rowIndex > offset) {
                    sql.append(",\n");
                }
                sql.append("  (");
                List<String> row = rows.get(rowIndex);
                for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                    if (columnIndex > 0) {
                        sql.append(", ");
                    }
                    String hex = row.get(columnIndex);
                    sql.append(sqlLiteral(hex, columns.get(columnIndex)));
                }
                sql.append(')');
            }
            sql.append(";\n");
        }
    }

    private static List<List<String>> readHexRows(
            Connection connection,
            String database,
            String table) throws SQLException {
        return readHexRows(connection, database, table, insertableColumns(connection, database, table));
    }

    private static List<List<String>> readHexRows(
            Connection connection,
            String database,
            String table,
            Set<String> ignoredColumns) throws SQLException {
        List<ColumnSpec> insertable = insertableColumns(connection, database, table);
        if (ignoredColumns.isEmpty()) {
            return readHexRows(connection, database, table, insertable);
        }
        List<ColumnSpec> comparable = insertable.stream()
                .filter(column -> !ignoredColumns.contains(column.name().toLowerCase(Locale.ROOT)))
                .toList();
        if (comparable.isEmpty() && !insertable.isEmpty()) {
            return readRowCardinality(connection, table);
        }
        return readHexRows(connection, database, table, comparable);
    }

    private static List<List<String>> readHexRows(
            Connection connection,
            String database,
            String table,
            List<ColumnSpec> columns) throws SQLException {
        if (columns.isEmpty()) {
            return List.of();
        }
        String expressions = columns.stream()
                .map(column -> "IF(" + quote(column.name()) + " IS NULL, NULL, HEX(CAST("
                        + quote(column.name()) + " AS BINARY)))")
                .reduce((a, b) -> a + ", " + b)
                .orElseThrow();
        List<List<String>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT " + expressions + " FROM " + quote(table))) {
            while (resultSet.next()) {
                List<String> row = new ArrayList<>(columns.size());
                for (int index = 1; index <= columns.size(); index++) {
                    row.add(resultSet.getString(index));
                }
                rows.add(Collections.unmodifiableList(row));
            }
        }
        rows.sort(MySqlBaselineStore::compareRows);
        return List.copyOf(rows);
    }

    private static List<List<String>> readRowCardinality(
            Connection connection,
            String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + quote(table))) {
            resultSet.next();
            int rowCount = Math.toIntExact(resultSet.getLong(1));
            return Collections.nCopies(rowCount, List.of());
        }
    }

    private static List<ColumnSpec> insertableColumns(
            Connection connection,
            String database,
            String table) throws SQLException {
        List<ColumnSpec> columns = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COLUMN_NAME, EXTRA, DATA_TYPE, CHARACTER_SET_NAME
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """)) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String extra = resultSet.getString(2).toUpperCase(Locale.ROOT);
                    if (!extra.contains("GENERATED")) {
                        columns.add(new ColumnSpec(
                                resultSet.getString(1),
                                literalCharacterSet(
                                        resultSet.getString(DATA_TYPE_COLUMN),
                                        resultSet.getString(CHARACTER_SET_COLUMN))));
                    }
                }
            }
        }
        return List.copyOf(columns);
    }

    private static String sqlLiteral(String hex, ColumnSpec column) {
        if (hex == null) {
            return "NULL";
        }
        String raw = "X'" + hex + "'";
        return column.literalCharacterSet() == null
                ? raw
                : "CONVERT(" + raw + " USING " + column.literalCharacterSet() + ")";
    }

    private static String literalCharacterSet(String dataType, String characterSet) {
        String normalizedType = dataType.toLowerCase(Locale.ROOT);
        if (Set.of(
                "binary",
                "varbinary",
                "tinyblob",
                "blob",
                "mediumblob",
                "longblob",
                "bit",
                "geometry").contains(normalizedType)) {
            return null;
        }
        if (characterSet != null && characterSet.matches(SAFE_CHARACTER_SET_PATTERN)) {
            return characterSet;
        }
        return "json".equals(normalizedType) ? "utf8mb4" : "ascii";
    }

    private static int compareRows(List<String> left, List<String> right) {
        for (int index = 0; index < left.size(); index++) {
            String leftValue = left.get(index);
            String rightValue = right.get(index);
            int comparison = Comparator.nullsFirst(String::compareTo).compare(leftValue, rightValue);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static String idempotentCreateTable(String statement) {
        return statement.replaceFirst("(?i)^CREATE\\s+TABLE\\s+", "CREATE TABLE IF NOT EXISTS ");
    }

    private static String idempotentCreateView(String statement) {
        return statement.replaceFirst("(?i)^CREATE\\s+", "CREATE OR REPLACE ");
    }

    private static String portableDefinition(String statement, String database) {
        String portable = DEFINER.matcher(statement).replaceAll("");
        return portable.replace("`" + database + "`.", "");
    }

    private static String normalizeDefinition(String statement, String database) {
        String portable = DEFINER.matcher(statement).replaceAll("");
        portable = AUTO_INCREMENT.matcher(portable).replaceAll("");
        portable = portable.replace("`" + database + "`.", "`${schema}`.");
        return portable.replaceAll("\\s+", " ").trim();
    }

    private static void assertNoUnsupportedObjects(Connection connection, String database)
            throws SQLException, MojoExecutionException {
        long routines = count(connection,
                "SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = ?", database);
        long events = count(connection,
                "SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA = ?", database);
        if (routines > 0 || events > 0) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-016 stored routines and events are not supported; routines="
                            + routines + ", events=" + events);
        }
    }

    private static long count(Connection connection, String sql, String database) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static boolean isHistoryTable(String table) {
        return table.startsWith("flyway_schema_history_")
                || table.startsWith("flyway_baseline_verify_")
                || table.startsWith("flyway_baseline_reentry_");
    }

    private static String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private static void validateDatabaseName(String database) throws MojoExecutionException {
        if (database == null || !database.matches(SAFE_DATABASE_NAME_PATTERN)) {
            throw new MojoExecutionException(
                    "MANGO-BASELINE-017 unsafe temporary database name: " + database);
        }
    }

    private static MojoExecutionException databaseFailure(String action, SQLException exception) {
        return new MojoExecutionException(
                "MANGO-BASELINE-018 failed to " + action + ": " + exception.getMessage(), exception);
    }

    record DatabaseIdentity(String product, String version) {
    }

    record SchemaSnapshot(
            Map<String, TableSnapshot> tables,
            Map<String, String> definitions,
            Map<String, List<List<String>>> data) {
    }

    record TableSnapshot(
            String engine,
            String characterSet,
            String collation,
            String rowFormat,
            String createOptions,
            String comment,
            Map<String, ColumnSnapshot> columns,
            Map<String, IndexSnapshot> indexes,
            Map<String, ConstraintSnapshot> constraints) {
    }

    record ColumnSnapshot(
            int ordinal,
            String name,
            String type,
            boolean nullable,
            String defaultValue,
            String extra,
            String generationExpression,
            String characterSet,
            String collation,
            String comment) {
    }

    record IndexSnapshot(
            boolean unique,
            String type,
            String comment,
            String indexComment,
            boolean visible,
            List<IndexColumnSnapshot> columns) {
    }

    record IndexColumnSnapshot(
            int ordinal,
            String name,
            String collation,
            Long prefixLength,
            boolean nullable,
            String expression) {
    }

    record ConstraintSnapshot(
            String type,
            boolean enforced,
            List<ConstraintColumnSnapshot> columns,
            ReferentialConstraintSnapshot reference,
            String checkClause) {
    }

    record ConstraintColumnSnapshot(
            int ordinal,
            Long referencedOrdinal,
            String name,
            String referencedSchema,
            String referencedTable,
            String referencedColumn) {
    }

    record ReferentialConstraintSnapshot(
            String uniqueConstraintSchema,
            String uniqueConstraintName,
            String matchOption,
            String updateRule,
            String deleteRule) {
    }

    private record TableHeader(
            String engine,
            String characterSet,
            String collation,
            String rowFormat,
            String createOptions,
            String comment) {
    }

    private record IndexHeader(
            boolean unique,
            String type,
            String comment,
            String indexComment,
            boolean visible) {
    }

    private record ConstraintHeader(String type, boolean enforced) {
    }

    private record DatabaseObjects(Map<String, String> tables, Map<String, String> views) {
    }

    private record TriggerDefinition(String name, String tableName, String createStatement) {
    }

    private record ColumnSpec(String name, String literalCharacterSet) {
    }

    private record AuditTemporalColumn(String name, String dataType) {
    }
}
