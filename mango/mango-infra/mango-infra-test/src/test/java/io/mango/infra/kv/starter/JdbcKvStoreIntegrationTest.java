package io.mango.infra.kv.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxTopics;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.kv.core.outbox.KvOutboxStore;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JdbcKvStore 数据库集成测试。
 *
 * 默认使用 H2 MySQL 兼容模式，也支持通过 kv.test.datasource.* 切换到隔离的真实 MySQL 测试库。
 */
@SpringBootTest(classes = {
        AopAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        KvStoreAutoConfiguration.class,
        JdbcKvStoreIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=${kv.test.datasource.url:jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}",
        "spring.datasource.username=${kv.test.datasource.username:sa}",
        "spring.datasource.password=${kv.test.datasource.password:}",
        "spring.datasource.driver-class-name=${kv.test.datasource.driver-class-name:org.h2.Driver}",
        "spring.flyway.enabled=false",
        "mango.kv.store.type=jdbc",
        "mango.kv.provider.redis.host=localhost",
        "mango.kv.provider.redis.port=6379"
})
class JdbcKvStoreIntegrationTest {

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
                assertTrue(kvStore.put("expire_key", "replaced", 3600));
                assertEquals("replaced", kvStore.get("expire_key"));
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

        @Test
        void setIfAbsent_concurrent_sameKey_noExceptions() throws Exception {
                int threadCount = 24;
                int roundsPerThread = 40;
                ExecutorService pool = Executors.newFixedThreadPool(threadCount);
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch doneLatch = new CountDownLatch(threadCount);
                AtomicInteger successCount = new AtomicInteger();
                AtomicInteger failureCount = new AtomicInteger();
                AtomicReference<Throwable> error = new AtomicReference<>();
                List<Runnable> tasks = new ArrayList<>();

                for (int i = 0; i < threadCount; i++) {
                        tasks.add(() -> {
                                try {
                                        startLatch.await();
                                        for (int j = 0; j < roundsPerThread; j++) {
                                                boolean success = kvStore.setIfAbsent("concurrency-hotspot", "v", 3600);
                                                if (success) {
                                                        successCount.incrementAndGet();
                                                } else {
                                                        failureCount.incrementAndGet();
                                                }
                                        }
                                } catch (Throwable t) {
                                        error.compareAndSet(null, t);
                                } finally {
                                        doneLatch.countDown();
                                }
                        });
                }

                tasks.forEach(pool::execute);
                startLatch.countDown();
                assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "concurrency test timeout");
                pool.shutdown();

                assertTrue(error.get() == null, () -> "unexpected concurrency exception: " + error.get());
                assertEquals(1, successCount.get(), "only one request should obtain the lock");
                assertEquals(threadCount * roundsPerThread,
                        successCount.get() + failureCount.get(), "every call should produce a return value");
                assertEquals("v", kvStore.get("concurrency-hotspot"));
        }

        @Test
        void outbox_concurrentWorkflowEnqueue_persistsEveryMessageWithoutExceptions() throws Exception {
                int concurrency = 5;
                int messagesPerWorker = 20;
                int messageCount = concurrency * messagesPerWorker;
                KvOutboxStore outboxStore = new KvOutboxStore(
                        kvStore,
                        new ObjectMapper().findAndRegisterModules());
                ExecutorService pool = Executors.newFixedThreadPool(concurrency);
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch doneLatch = new CountDownLatch(messageCount);
                AtomicReference<Throwable> error = new AtomicReference<>();
                List<OutboxMessage> messages = new ArrayList<>();

                for (int i = 0; i < messageCount; i++) {
                        OutboxMessage message = OutboxMessage.builder()
                                .topic(OutboxTopics.DOMAIN_EVENT)
                                .eventType("workflow.task.completed")
                                .businessType("workflow")
                                .businessKey("IT_ISSUE_452_" + i)
                                .aggregateId("IT_PROCESS_" + i)
                                .occurredAt(Instant.parse("2026-07-13T05:00:00Z").plusMillis(i))
                                .build();
                        messages.add(message);
                        pool.execute(() -> {
                                try {
                                        startLatch.await();
                                        outboxStore.enqueue(message);
                                } catch (Throwable t) {
                                        error.compareAndSet(null, t);
                                } finally {
                                        doneLatch.countDown();
                                }
                        });
                }

                startLatch.countDown();
                assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "concurrent outbox enqueue timeout");
                pool.shutdown();

                assertNull(error.get(), () -> "unexpected outbox concurrency exception: " + error.get());
                messages.forEach(message -> assertEquals(
                        message.getMessageId(),
                        outboxStore.findById(message.getMessageId()).getMessageId()));
                assertEquals(messageCount, outboxStore.claimByTopic(
                        "workflow-worker",
                        OutboxTopics.DOMAIN_EVENT,
                        messageCount,
                        Instant.parse("2026-07-13T05:01:00Z")).size());
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
