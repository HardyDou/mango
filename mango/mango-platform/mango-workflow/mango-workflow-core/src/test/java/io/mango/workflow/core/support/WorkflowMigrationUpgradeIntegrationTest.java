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
    private static final int MAVEN_1_0_21_22_V1_CHECKSUM = -1500222187;
    private static final int CURRENT_V1_CHECKSUM = 1010539203;
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

        assertThat(migrationResult.migrationsExecuted).isEqualTo(6);
        assertThat(historyChecksum("1")).isEqualTo(CURRENT_V1_CHECKSUM);
        assertThat(historyChecksum("2")).isNotNull();
        assertCanonicalAuditColumns();
        assertCurrentTaskReleaseColumns();
        // 夹具只保留 5 张受影响表，工作流申请中心表不在其中；V4 必须跳过而不是中断升级。
        assertThat(columnDefinition(new AuditColumn("workflow_business_apply", "affinity_key"))).isNull();
        assertThat(currentFlyway().migrate().migrationsExecuted).isZero();
    }

    @Test
    void migrate_1_0_21_22Checksum_repairsKnownHistory() throws SQLException {
        assertThat(releasedV1Flyway().migrate().migrationsExecuted).isOne();
        replaceV1Checksum(MAVEN_1_0_21_22_V1_CHECKSUM);

        var migrationResult = currentFlyway().migrate();

        assertThat(migrationResult.migrationsExecuted).isEqualTo(6);
        assertThat(historyChecksum("1")).isEqualTo(CURRENT_V1_CHECKSUM);
        assertThat(historyChecksum("2")).isNotNull();
        assertCanonicalAuditColumns();
        assertCurrentTaskReleaseColumns();
    }

    @Test
    void migrate_currentFreshV1_keepsExistingAuditColumnsAndRecordsV2() throws SQLException {
        var firstMigration = currentFlyway().migrate();
        var secondMigration = currentFlyway().migrate();

        assertThat(firstMigration.migrationsExecuted).isEqualTo(7);
        assertThat(secondMigration.migrationsExecuted).isZero();
        assertThat(historyChecksum("1")).isEqualTo(CURRENT_V1_CHECKSUM);
        assertThat(historyChecksum("2")).isNotNull();
        assertCanonicalAuditColumns();
        assertAffinityKeyColumn();
        assertCurrentTaskReleaseColumns();
        assertCurrentTaskUniqueKey();
    }

    @Test
    void migrate_currentTaskDuplicates_keepsNewestRowAndAddsUniqueKey() throws SQLException {
        assertThat(flywayToVersion("6").migrate().migrationsExecuted).isEqualTo(6);
        insertCurrentTaskRow(9001L, "1", "T-DUP", "2026-01-01 10:00:00");
        insertCurrentTaskRow(9002L, "1", "T-DUP", "2026-01-01 10:05:00");
        insertCurrentTaskRow(9003L, "1", "T-DUP", "2026-01-01 10:05:00");
        insertCurrentTaskRow(9004L, "2", "T-DUP", "2026-01-01 10:00:00");
        insertCurrentTaskRow(9005L, "1", null, "2026-01-01 10:00:00");
        insertCurrentTaskRow(9006L, "1", null, "2026-01-01 10:00:00");
        assertThat(currentTaskRowIds("1", "T-DUP")).containsExactlyInAnyOrder(9001L, 9002L, 9003L);

        var migrationResult = currentFlyway().migrate();

        assertThat(migrationResult.migrationsExecuted).isOne();
        assertThat(currentTaskRowIds("1", "T-DUP"))
                .as("保留 updated_at 最新的一行，updated_at 相同时保留 id 最大的一行")
                .containsExactly(9003L);
        assertThat(currentTaskRowIds("2", "T-DUP")).containsExactly(9004L);
        assertThat(currentTaskRowIds("1", null))
                .as("task_id 为空的行不构成重复，唯一键允许多个 NULL")
                .containsExactlyInAnyOrder(9005L, 9006L);
        assertCurrentTaskUniqueKey();
        assertThat(currentFlyway().migrate().migrationsExecuted).isZero();
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

    private void assertAffinityKeyColumn() throws SQLException {
        assertThat(columnDefinition(new AuditColumn("workflow_business_apply", "affinity_key")))
                .as("workflow_business_apply.affinity_key added by V4")
                .isNotNull();
    }

    private void assertCurrentTaskUniqueKey() throws SQLException {
        String sql = """
                SELECT column_name
                FROM information_schema.statistics
                WHERE table_schema = ? AND table_name = 'workflow_business_apply_current_task'
                  AND index_name = 'uk_workflow_business_apply_current_task' AND non_unique = 0
                ORDER BY seq_in_index
                """;
        try (Connection connection = databaseConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, databaseName());
            try (ResultSet resultSet = statement.executeQuery()) {
                var columns = new java.util.ArrayList<String>();
                while (resultSet.next()) {
                    columns.add(resultSet.getString("column_name"));
                }
                assertThat(columns).as("V7 建的 (tenant_id, task_id) 唯一键").containsExactly("tenant_id", "task_id");
            }
        }
    }

    private void insertCurrentTaskRow(long id, String tenantId, String taskId, String updatedAt) throws SQLException {
        String sql = """
                INSERT INTO workflow_business_apply_current_task
                  (id, tenant_id, apply_id, business_type, business_key, task_id, claim_status, created_at, updated_at)
                VALUES (?, ?, 20, 'BAOHAN_RC_PROJECT_REVIEW', 'RC1', ?, 'ASSIGNED', ?, ?)
                """;
        try (Connection connection = databaseConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, tenantId);
            statement.setString(3, taskId);
            statement.setString(4, updatedAt);
            statement.setString(5, updatedAt);
            assertThat(statement.executeUpdate()).isOne();
        }
    }

    private List<Long> currentTaskRowIds(String tenantId, String taskId) throws SQLException {
        String sql = taskId == null
                ? "SELECT id FROM workflow_business_apply_current_task WHERE tenant_id = ? AND task_id IS NULL ORDER BY id"
                : "SELECT id FROM workflow_business_apply_current_task WHERE tenant_id = ? AND task_id = ? ORDER BY id";
        try (Connection connection = databaseConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tenantId);
            if (taskId != null) {
                statement.setString(2, taskId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                var ids = new java.util.ArrayList<Long>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id"));
                }
                return ids;
            }
        }
    }

    private void assertCurrentTaskReleaseColumns() throws SQLException {
        assertThat(columnDefinition(new AuditColumn("workflow_business_apply_current_task", "assign_source")))
                .as("workflow_business_apply_current_task.assign_source added by V6")
                .extracting("dataType", "nullable")
                .containsExactly("varchar", "YES");
        assertThat(columnDefinition(new AuditColumn("workflow_business_apply_current_task", "claim_time")))
                .as("workflow_business_apply_current_task.claim_time added by V6")
                .extracting("dataType", "nullable")
                .containsExactly("datetime", "YES");
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

    private static Flyway flywayToVersion(String version) {
        return Flyway.configure()
                .dataSource(databaseUrl(), requiredEnvironment("MANGO_DB_USERNAME"),
                        environment("MANGO_DB_PASSWORD", ""))
                .locations("classpath:db/migration/workflow")
                .table(HISTORY_TABLE)
                .target(version)
                .load();
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
