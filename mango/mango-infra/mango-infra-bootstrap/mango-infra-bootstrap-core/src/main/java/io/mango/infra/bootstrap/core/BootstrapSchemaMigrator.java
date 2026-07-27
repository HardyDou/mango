package io.mango.infra.bootstrap.core;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;

public final class BootstrapSchemaMigrator {

    private final DataSource dataSource;

    public BootstrapSchemaMigrator(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public void migrate() {
        assertMySql();
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/bootstrap")
                .table("flyway_schema_history_bootstrap")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load()
                .migrate();
    }

    private void assertMySql() {
        try (var connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                throw new IllegalStateException("Mango Bootstrap currently requires MySQL: database=" + product);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Inspect Mango Bootstrap database failed", exception);
        }
    }
}
