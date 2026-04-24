package io.mango.infra.kv.starter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.aop.support.AopUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JdbcKvStore 集成测试 - H2 内存数据库（MySQL 兼容模式）
 *
 * 验证 JdbcKvStore 在 H2（MySQL 兼容模式）下的真实 SQL 执行。
 * 复用 MySQL 版 SQL 脚本，无需单独写 H2 SQL。
 */
@SpringBootTest(classes = {
        AopAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        KvStoreAutoConfiguration.class,
        JdbcKvStoreH2IntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.kv.store.type=jdbc",
        "mango.kv.provider.redis.host=localhost",
        "mango.kv.provider.redis.port=6379"
})
class JdbcKvStoreH2IntegrationTest {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private IKvStore kvStore;

        @Autowired
        private FailingJdbcKvWriter failingWriter;

        @BeforeEach
        void setUp() {
                jdbcTemplate.execute("DROP TABLE IF EXISTS infra_kv_entry");
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

        @Test
        void put_and_get() {
                assertTrue(kvStore.put("key1", "value1", 3600));
                assertEquals("value1", kvStore.get("key1"));
        }

        @Test
        void put_duplicate_returnsFalse() {
                kvStore.put("key2", "value2", 3600);
                assertFalse(kvStore.put("key2", "value2", 3600));
        }

        @Test
        void get_nonExistent_returnsNull() {
                assertNull(kvStore.get("non_existent_key"));
        }

        @Test
        void delete_existing() {
                kvStore.put("key3", "value3", 3600);
                kvStore.delete("key3");
                assertNull(kvStore.get("key3"));
        }

        @Test
        void exists_existing_returnsTrue() {
                kvStore.put("key4", "value4", 3600);
                assertTrue(kvStore.exists("key4"));
        }

        @Test
        void exists_nonExisting_returnsFalse() {
                assertFalse(kvStore.exists("non_existent"));
        }

        @Test
        void increment_createsCounter() {
                long count = kvStore.increment("counter1", 60);
                assertEquals(1, count);
                count = kvStore.increment("counter1", 60);
                assertEquals(2, count);
        }

        @Test
        void put_expiredKey_canOverwrite() throws InterruptedException {
                kvStore.put("expire_key", "val", 1);
                assertTrue(kvStore.exists("expire_key"));
                Thread.sleep(1100);
                assertFalse(kvStore.exists("expire_key"));
        }

        @Test
        void jdbcStore_isSpringTransactionalProxy() {
                assertTrue(AopUtils.isAopProxy(kvStore));
                assertTrue(AopUtils.getTargetClass(kvStore).equals(JdbcKvStore.class));
        }

        @Test
        void transactionalRollback_restoresPreviousValueWhenOuterTransactionFails() {
                kvStore.set("rollback:key", "before", 3600);

                assertThrows(IllegalStateException.class, () -> failingWriter.writeThenFail("rollback:key", "after"));

                assertEquals("before", kvStore.get("rollback:key"));
        }

        @Configuration(proxyBeanMethods = false)
        @EnableTransactionManagement
        static class TestConfig {

                @Bean
                PlatformTransactionManager platformTransactionManager(javax.sql.DataSource dataSource) {
                        return new DataSourceTransactionManager(dataSource);
                }

                @Bean
                FailingJdbcKvWriter failingJdbcKvWriter(IKvStore kvStore) {
                        return new FailingJdbcKvWriter(kvStore);
                }
        }

        static class FailingJdbcKvWriter {

                private final IKvStore kvStore;

                FailingJdbcKvWriter(IKvStore kvStore) {
                        this.kvStore = kvStore;
                }

                @Transactional
                public void writeThenFail(String key, String value) {
                        kvStore.set(key, value, 3600);
                        throw new IllegalStateException("rollback");
                }
        }
}
