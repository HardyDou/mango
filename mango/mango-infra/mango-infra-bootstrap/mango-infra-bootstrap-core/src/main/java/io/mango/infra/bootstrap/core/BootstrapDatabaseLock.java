package io.mango.infra.bootstrap.core;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public final class BootstrapDatabaseLock {

    private final DataSource dataSource;

    public BootstrapDatabaseLock(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public Lease acquire(String environmentKey, int timeoutSeconds) {
        try {
            Connection connection = dataSource.getConnection();
            String product = productName(connection.getMetaData());
            if (product.contains("mysql")) {
                return acquireMysql(connection, lockName(environmentKey), timeoutSeconds);
            }
            connection.close();
            throw new IllegalStateException("Mango Bootstrap currently requires MySQL: database=" + product);
        } catch (SQLException exception) {
            throw new IllegalStateException("Acquire bootstrap database lock failed", exception);
        }
    }

    private static Lease acquireMysql(Connection connection, String name, int timeoutSeconds) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, name);
            statement.setInt(2, Math.max(0, timeoutSeconds));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    connection.close();
                    throw new IllegalStateException("Bootstrap lock is held by another process: " + name);
                }
            }
        }
        return new Lease(connection, "SELECT RELEASE_LOCK(?)", name);
    }

    private static String productName(DatabaseMetaData metadata) throws SQLException {
        return metadata.getDatabaseProductName().toLowerCase();
    }

    private static String lockName(String environmentKey) {
        String normalized = environmentKey == null ? "default" : environmentKey.trim();
        String value = "mango-bootstrap:" + normalized;
        return value.length() <= 64 ? value : value.substring(0, 64);
    }

    public static final class Lease implements AutoCloseable {

        private final Connection connection;
        private final String releaseSql;
        private final String name;
        private boolean closed;

        private Lease(Connection connection, String releaseSql, String name) {
            this.connection = connection;
            this.releaseSql = releaseSql;
            this.name = name;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try (PreparedStatement statement = connection.prepareStatement(releaseSql)) {
                statement.setString(1, name);
                statement.execute();
            } catch (SQLException ignored) {
                // Closing the connection also releases session-scoped advisory locks.
            } finally {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // Nothing useful can be done while releasing a best-effort lease.
                }
            }
        }
    }
}
