package io.mango.infra.kv.starter;

import io.mango.infra.kv.core.JdbcKvStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JdbcKvStore 集成测试 - H2 内存数据库（MySQL 兼容模式）
 *
 * 验证 JdbcKvStore 在 H2（MySQL 兼容模式）下的真实 SQL 执行。
 * 复用 MySQL 版 SQL 脚本，无需单独写 H2 SQL。
 */
@SpringBootTest(classes = {DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class, KvStoreAutoConfiguration.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.kv.type=db",
        "mango.kv.provider.redis.host=localhost",
        "mango.kv.provider.redis.port=6379"
})
class JdbcKvStoreH2IntegrationTest {

        @MockBean
        private RedissonClient redissonClient;

        @MockBean
        private RAtomicLong atomicLong;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private JdbcKvStore store;

        @BeforeEach
        void setUp() {
                when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
                // Use a long sequence to avoid exhaustion across all tests
                when(atomicLong.incrementAndGet()).thenReturn(
                        1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
                        11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L);
                when(atomicLong.get()).thenReturn(1L);

                jdbcTemplate.execute("DROP TABLE IF EXISTS sys_kv_record");
                jdbcTemplate.execute("""
                        CREATE TABLE sys_kv_record (
                            id          BIGINT NOT NULL,
                            kv_key      VARCHAR(200) NOT NULL,
                            kv_value    TEXT,
                            expire_time DATETIME NOT NULL,
                            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_kv_key (kv_key)
                        )
                        """);

                store = new JdbcKvStore(jdbcTemplate, redissonClient);
        }

        @Test
        void put_and_get() {
                assertTrue(store.put("key1", "value1", 3600));
                assertEquals("value1", store.get("key1"));
        }

        @Test
        void put_duplicate_returnsFalse() {
                store.put("key2", "value2", 3600);
                assertFalse(store.put("key2", "value2", 3600));
        }

        @Test
        void get_nonExistent_returnsNull() {
                assertNull(store.get("non_existent_key"));
        }

        @Test
        void delete_existing() {
                store.put("key3", "value3", 3600);
                store.delete("key3");
                assertNull(store.get("key3"));
        }

        @Test
        void exists_existing_returnsTrue() {
                store.put("key4", "value4", 3600);
                assertTrue(store.exists("key4"));
        }

        @Test
        void exists_nonExisting_returnsFalse() {
                assertFalse(store.exists("non_existent"));
        }

        @Test
        void increment_createsCounter() {
                long count = store.increment("counter1", 60);
                assertEquals(1, count);
                count = store.increment("counter1", 60);
                assertEquals(2, count);
        }

        @Test
        void put_expiredKey_canOverwrite() throws InterruptedException {
                store.put("expire_key", "val", 1);
                assertTrue(store.exists("expire_key"));
                Thread.sleep(1100);
                assertFalse(store.exists("expire_key"));
        }
}
