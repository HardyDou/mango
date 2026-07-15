package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ICounter;
import io.mango.infra.kv.api.IKvSortedSet;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxTopics;
import io.mango.infra.kv.core.capability.KvStoreCounter;
import io.mango.infra.kv.core.outbox.KvOutboxStore;
import io.mango.infra.kv.core.redis.RedisKvStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Redis integration tests.
 * Requires Redis running on localhost:6379 with no password.
 */
class RedisRealIntegrationTest {

    private static final String KEY_PREFIX = "rt:" + System.currentTimeMillis() + ":";

    private static RedissonClient redisson;
    private static RedisKvStore kvStore;
    private IKvSortedSet sortedSet;
    private ICounter counter;

    @BeforeAll
    static void beforeAll() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://localhost:6379")
            .setDatabase(0)
            .setConnectTimeout(5000)
            .setTimeout(3000)
            .setConnectionMinimumIdleSize(2)
            .setConnectionPoolSize(8);
        redisson = Redisson.create(config);
        kvStore = new RedisKvStore(redisson);
    }

    @AfterAll
    static void afterAll() {
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        redisson.getKeys().deleteByPattern(KEY_PREFIX + "*");
        sortedSet = kvStore;
        counter = new KvStoreCounter(kvStore);
    }

    @Test
    void sortedSet_addRangeAndRemoveByScore() {
        String key = KEY_PREFIX + "zset:presence:user:1001";

        sortedSet.add(key, "s2", 20, 60);
        sortedSet.add(key, "s1", 10, 60);
        sortedSet.add(key, "s3", 30, 60);

        assertThat(sortedSet.rangeByScore(key, 0, 100, 0)).containsExactly("s1", "s2", "s3");
        assertThat(sortedSet.removeByScore(key, 0, 20)).isEqualTo(2);
        assertThat(sortedSet.rangeByScore(key, 0, 100, 0)).containsExactly("s3");
    }

    @Test
    void sortedSet_addRefreshesRedisKeyTtl() throws InterruptedException {
        String key = KEY_PREFIX + "zset:ttl";

        sortedSet.add(key, "s1", 10, 1);
        assertThat(redisson.getKeys().remainTimeToLive(key)).isPositive();

        Thread.sleep(700);
        sortedSet.add(key, "s2", 20, 2);

        long ttlMillis = redisson.getKeys().remainTimeToLive(key);
        assertThat(ttlMillis).isGreaterThan(Duration.ofSeconds(1).toMillis());
        assertThat(sortedSet.rangeByScore(key, 0, 100, 0)).containsExactly("s1", "s2");
    }

    @Test
    void concurrent_increment_noRaceCondition() throws InterruptedException {
        String key = KEY_PREFIX + "counter:concurrent";
        int threadCount = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.increment(key, 1, 60);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount * incrementsPerThread);
        assertThat(counter.get(key)).isEqualTo(threadCount * incrementsPerThread);
    }

    @Test
    void incrementBy_doesNotRefreshFixedWindowWhenCounterReturnsToZero() throws InterruptedException {
        String key = KEY_PREFIX + "counter:fixed-window";

        assertThat(kvStore.incrementBy(key, 1, 4)).isEqualTo(1);
        Thread.sleep(1100);
        assertThat(kvStore.incrementBy(key, -1, 4)).isZero();
        long ttlBeforeIncrement = redisson.getKeys().remainTimeToLive(key);

        assertThat(kvStore.incrementBy(key, 1, 4)).isEqualTo(1);
        long ttlAfterIncrement = redisson.getKeys().remainTimeToLive(key);

        assertThat(ttlAfterIncrement).isLessThanOrEqualTo(ttlBeforeIncrement + 200);
    }

    @Test
    void outbox_concurrentClaim_assignsMessageToOnlyOneWorker() throws InterruptedException {
        KvOutboxStore outboxStore = new KvOutboxStore(
                kvStore,
                new ObjectMapper().findAndRegisterModules());
        OutboxMessage message = OutboxMessage.builder()
                .topic(OutboxTopics.DOMAIN_EVENT)
                .eventType("kv.concurrent.claim")
                .occurredAt(java.time.Instant.now())
                .build();
        outboxStore.enqueue(message);
        int workerCount = 24;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workerCount);
        AtomicInteger claimedCount = new AtomicInteger();

        for (int i = 0; i < workerCount; i++) {
            String workerId = "worker-" + i;
            executor.submit(() -> {
                try {
                    start.await();
                    claimedCount.addAndGet(outboxStore.claimByTopic(
                            workerId,
                            OutboxTopics.DOMAIN_EVENT,
                            1,
                            java.time.Instant.now()).size());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(claimedCount.get()).isEqualTo(1);
    }
}
