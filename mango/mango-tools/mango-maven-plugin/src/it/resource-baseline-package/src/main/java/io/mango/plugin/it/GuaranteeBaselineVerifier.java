package io.mango.plugin.it;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GuaranteeBaselineVerifier {

    private GuaranteeBaselineVerifier() {
    }

    public static void main(String[] args) throws Exception {
        String adminUrl = requiredEnvironment("MANGO_BASELINE_TEST_DB_URL");
        String username = System.getenv().getOrDefault("MANGO_BASELINE_TEST_DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("MANGO_BASELINE_TEST_DB_PASSWORD", "");
        String database = "mango_resource_restore_it_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String databaseUrl = databaseUrl(adminUrl, database);
        try {
            execute(adminUrl, username, password, "CREATE DATABASE `" + database
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            runApplication(databaseUrl, username, password);
            assertCount(databaseUrl, username, password, "guarantee_product", 1);
            assertCount(databaseUrl, username, password, "guarantee_env_endpoint", 1);
            assertCount(databaseUrl, username, password, "resource_registry", 2);
            assertCount(databaseUrl, username, password, "resource_sync_log", 1);
            assertCount(databaseUrl, username, password, "resource_change_log", 1);
            assertCount(databaseUrl, username, password, "resource_module_receipt", 1);
            assertCount(databaseUrl, username, password, "guarantee_business_bootstrap", 1);
            assertQueryCount(databaseUrl, username, password, """
                    SELECT COUNT(*)
                      FROM resource_sync_log log
                      JOIN resource_registry registry ON registry.id = log.resource_id
                     WHERE registry.resource_id = '851002'
                    """, 1);
            assertQueryCount(databaseUrl, username, password, """
                    SELECT COUNT(*)
                      FROM resource_sync_log log
                      JOIN resource_registry registry ON registry.id = log.resource_id
                     WHERE registry.resource_id = '851001'
                    """, 0);
            assertQueryCount(databaseUrl, username, password, """
                    SELECT COUNT(*) FROM resource_registry
                     WHERE resource_id IN ('851001', '851002') AND CHAR_LENGTH(source_hash) = 32
                    """, 2);

            runApplication(databaseUrl, username, password);
            assertCount(databaseUrl, username, password, "resource_sync_log", 1);
            assertCount(databaseUrl, username, password, "resource_change_log", 1);
            assertCount(databaseUrl, username, password, "guarantee_business_bootstrap", 1);
            System.out.println("RESOURCE_DATABASE_BASELINE_VERIFIED");
        } finally {
            execute(adminUrl, username, password, "DROP DATABASE IF EXISTS `" + database + "`");
        }
    }

    private static void runApplication(String databaseUrl, String username, String password)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(GuaranteeBaselineApplication.class.getName());
        command.add("bootstrap");
        command.add("apply");
        command.add("--mango.bootstrap.strategy=cold");
        command.add("--mango.bootstrap.environment-key=resource-restore-it");
        command.add("--mango.release.id=resource-restore-it");
        command.add("--mango.release.revision=verified");
        command.add("--mango.release.generation=1");
        command.add("--spring.main.banner-mode=off");
        ProcessBuilder builder = new ProcessBuilder(command).inheritIO();
        Map<String, String> environment = builder.environment();
        environment.put("SPRING_DATASOURCE_URL", databaseUrl);
        environment.put("SPRING_DATASOURCE_USERNAME", username);
        environment.put("SPRING_DATASOURCE_PASSWORD", password);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Restored baseline application failed: exitCode=" + exitCode);
        }
    }

    private static void assertCount(
            String url, String username, String password, String table, long expected) throws SQLException {
        assertQueryCount(url, username, password, "SELECT COUNT(*) FROM `" + table + "`", expected);
    }

    private static void assertQueryCount(
            String url, String username, String password, String sql, long expected) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            long actual = resultSet.getLong(1);
            if (actual != expected) {
                throw new IllegalStateException(
                        "Unexpected query count: expected=" + expected + ", actual=" + actual + ", sql=" + sql);
            }
        }
    }

    private static void execute(String url, String username, String password, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String databaseUrl(String adminUrl, String database) {
        int query = adminUrl.indexOf('?');
        String suffix = query < 0 ? "" : adminUrl.substring(query);
        String withoutQuery = query < 0 ? adminUrl : adminUrl.substring(0, query);
        int slash = withoutQuery.lastIndexOf('/');
        return withoutQuery.substring(0, slash + 1) + database + suffix;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
