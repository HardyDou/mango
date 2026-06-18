package io.mango.resource.core.sync;

import io.mango.infra.kv.core.capability.KvStoreLocker;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceRegistryLockTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:resource_lock_" + System.nanoTime()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        jdbcTemplate = new JdbcTemplate(dataSource);
        createKvTable();
    }

    @Test
    void lockUsesJdbcKvStoreSemantics() {
        ResourceRegistryLock registryLock = new ResourceRegistryLock(
                new KvStoreLocker(new JdbcKvStore(jdbcTemplate)));

        assertThat(registryLock.tryLock("node-a", 60)).isTrue();
        assertThat(registryLock.tryLock("node-b", 60)).isFalse();
        assertThat(countLockRows()).isEqualTo(1);

        registryLock.unlock("node-a");

        assertThat(countLockRows()).isZero();
        assertThat(registryLock.tryLock("node-b", 60)).isTrue();
    }

    private void createKvTable() {
        jdbcTemplate.execute("""
                CREATE TABLE infra_kv_entry (
                    id          BIGINT NOT NULL,
                    kv_key      VARCHAR(200) NOT NULL,
                    kv_value    TEXT,
                    expire_time DATETIME NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_kv_key (kv_key)
                )
                """);
    }

    private long countLockRows() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM infra_kv_entry
                WHERE kv_key = ?
                """, Long.class, ResourceRegistryLock.LOCK_NAME);
    }
}
