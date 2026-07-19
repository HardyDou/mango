package io.mango.workflow.core.support;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "MANGO_DB_NAME", matches = "mango_dev_.*")
class WorkflowMigrationUpgradeIntegrationTest {

    private static final String HISTORY_TABLE = "flyway_schema_history_workflow";
    private static final int MAVEN_1_0_20_V1_CHECKSUM = -840523381;
    private static final int CURRENT_V1_CHECKSUM = -1500222187;
    private static final int UNKNOWN_V1_CHECKSUM = 506;
    private static final List<AuditColumn> AUDIT_COLUMNS = List.of(
            new AuditColumn("workflow_task_record", "created_by"),
            new AuditColumn("workflow_copied_task", "created_by"),
            new AuditColumn("workflow_business_apply_current_task", "created_by"),
            new AuditColumn("workflow_business_apply_current_task", "updated_by"),
            new AuditColumn("workflow_business_apply_status_log", "created_by"),
            new AuditColumn("workflow_business_apply_status_log", "updated_by"),
            new AuditColumn("workflow_business_apply_status_log", "updated_at")
    );

    @BeforeEach
    void rebuildDatabase() throws SQLException {
        executeOnServer("DROP DATABASE IF EXISTS `" + databaseName() + "`");
        executeOnServer("CREATE DATABASE `" + databaseName()
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
    }

    @AfterAll
    static void dropDatabase() throws SQLException {
        executeOnServer("DROP DATABASE IF EXISTS `" + databaseName() + "`");
    }

    @Test
    void migrate_1_0_20SchemaAndChecksum_addsMissingAuditColumns() throws SQLException {
        assertThat(releasedV1Flyway().migrate().migrationsExecuted).isOne();
        replaceV1Checksum(MAVEN_1_0_20_V1_CHECKSUM);
        assertThat(AUDIT_COLUMNS).allSatisfy(column -> assertThat(columnExists(column)).isFalse());

        var migrationResult = currentFlyway().migrate();

        assertThat(migrationResult.migrationsExecuted).isOne();
        assertThat(historyChecksum("1")).isEqualTo(CURRENT_V1_CHECKSUM);
        assertThat(historyChecksum("2")).isNotNull();
        assertCanonicalAuditColumns();
        assertThat(currentFlyway().migrate().migrationsExecuted).isZero();
    }

    @Test
    void migrate_currentFreshV1_keepsExistingAuditColumnsAndRecordsV2() throws SQLException {
        var firstMigration = currentFlyway().migrate();
        var secondMigration = currentFlyway().migrate();

        assertThat(firstMigration.migrationsExecuted).isEqualTo(2);
        assertThat(secondMigration.migrationsExecuted).isZero();
        assertThat(historyChecksum("1")).isEqualTo(CURRENT_V1_CHECKSUM);
        assertThat(historyChecksum("2")).isNotNull();
        assertCanonicalAuditColumns();
    }

    @Test
    void migrate_unknownV1Checksum_failsClosedWithoutChangingSchema() throws SQLException {
        assertThat(releasedV1Flyway().migrate().migrationsExecuted).isOne();
        replaceV1Checksum(UNKNOWN_V1_CHECKSUM);

        assertThatThrownBy(() -> currentFlyway().migrate())
                .hasMessageContaining("Migration checksum mismatch for migration version 1");
        assertThat(historyChecksum("1")).isEqualTo(UNKNOWN_V1_CHECKSUM);
        assertThat(AUDIT_COLUMNS).allSatisfy(column -> assertThat(columnExists(column)).isFalse());
    }

    private void assertCanonicalAuditColumns() throws SQLException {
        for (AuditColumn column : AUDIT_COLUMNS) {
            ColumnDefinition definition = columnDefinition(column);
            assertThat(definition).as(column.tableName() + "." + column.columnName()).isNotNull();
            assertThat(definition.dataType()).isEqualTo(column.columnName().equals("updated_at")
                    ? "datetime"
                    : "bigint");
            assertThat(definition.nullable()).isEqualTo(column.columnName().equals("updated_at")
                    ? "NO"
                    : "YES");
            if (column.columnName().equals("updated_at")) {
                assertThat(definition.defaultValue()).isEqualTo("CURRENT_TIMESTAMP");
                assertThat(definition.extra()).isEqualTo("DEFAULT_GENERATED on update CURRENT_TIMESTAMP");
            }
        }
    }

    private boolean columnExists(AuditColumn column) throws SQLException {
        return columnDefinition(column) != null;
    }

    private ColumnDefinition columnDefinition(AuditColumn column) throws SQLException {
        String sql = """
                SELECT data_type, is_nullable, column_default, extra
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ? AND column_name = ?
                """;
        try (Connection connection = databaseConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, databaseName());
            statement.setString(2, column.tableName());
            statement.setString(3, column.columnName());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new ColumnDefinition(
                        resultSet.getString("data_type"),
                        resultSet.getString("is_nullable"),
                        resultSet.getString("column_default"),
                        resultSet.getString("extra"));
            }
        }
    }

    private Integer historyChecksum(String version) throws SQLException {
        String sql = "SELECT checksum FROM `" + HISTORY_TABLE + "` WHERE version = ? AND success = 1";
        try (Connection connection = databaseConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getInt("checksum");
            }
        }
    }

    private void replaceV1Checksum(int checksum) throws SQLException {
        String sql = "UPDATE `" + HISTORY_TABLE + "` SET checksum = ? WHERE version = '1' AND success = 1";
        try (Connection connection = databaseConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, checksum);
            assertThat(statement.executeUpdate()).isOne();
        }
    }

    private static Flyway releasedV1Flyway() {
        return flyway("classpath:db/migration/workflow-1.0.20");
    }

    private static Flyway currentFlyway() {
        return flyway("classpath:db/migration/workflow");
    }

    private static Flyway flyway(String location) {
        return Flyway.configure()
                .dataSource(databaseUrl(), requiredEnvironment("MANGO_DB_USERNAME"),
                        environment("MANGO_DB_PASSWORD", ""))
                .locations(location)
                .table(HISTORY_TABLE)
                .load();
    }

    private static Connection databaseConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl(), requiredEnvironment("MANGO_DB_USERNAME"),
                environment("MANGO_DB_PASSWORD", ""));
    }

    private static void executeOnServer(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(serverUrl(),
                requiredEnvironment("MANGO_DB_USERNAME"), environment("MANGO_DB_PASSWORD", ""));
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String databaseName() {
        String workspaceDatabase = requiredEnvironment("MANGO_DB_NAME");
        if (!workspaceDatabase.startsWith("mango_dev_")) {
            throw new IllegalStateException("Workflow migration test requires a mango_dev_* database");
        }
        return "mango_dev_506_" + Integer.toUnsignedString(workspaceDatabase.hashCode(), 36);
    }

    private static String databaseUrl() {
        return serverUrl() + databaseName() + "?useSSL=false&allowPublicKeyRetrieval=true";
    }

    private static String serverUrl() {
        return "jdbc:mysql://" + environment("MANGO_DB_HOST", "127.0.0.1") + ":"
                + environment("MANGO_DB_PORT", "3306") + "/";
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record AuditColumn(String tableName, String columnName) {
    }

    private record ColumnDefinition(String dataType, String nullable, String defaultValue, String extra) {
        private ColumnDefinition {
            dataType = dataType.toLowerCase(Locale.ROOT);
        }
    }
}
