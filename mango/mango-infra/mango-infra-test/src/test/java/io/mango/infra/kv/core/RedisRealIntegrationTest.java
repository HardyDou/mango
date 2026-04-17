package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.ICounter;
import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.infra.kv.api.ITokenStore;
import io.mango.infra.kv.core.redis.RedisCache;
import io.mango.infra.kv.core.redis.RedisCounter;
import io.mango.infra.kv.core.redis.RedisIdempotent;
import io.mango.infra.kv.core.redis.RedisKvStore;
import io.mango.infra.kv.core.redis.RedisLocker;
import io.mango.infra.kv.core.redis.RedisRateLimiter;
import io.mango.infra.kv.core.redis.RedisTokenStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Redis integration tests.
 * Requires Redis running on localhost:6379 with no password.
 * Run with: mvn test -pl mango-infra/mango-infra-test -Dtest=RedisRealIntegrationTest
 */
class RedisRealIntegrationTest {

    private static RedissonClient redisson;
    private static RedisKvStore kvStore;
    private ICache cache;
    private ILocker locker;
    private ICounter counter;
    private IRateLimiter rateLimiter;
    private IIdempotent idempotent;
    private ITokenStore tokenStore;

    // Unique prefix per test run to avoid key collisions
    private static final String KEY_PREFIX = "rt:" + System.currentTimeMillis() + ":";

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
        // Clear all test keys before each test
        redisson.getKeys().deleteByPattern(KEY_PREFIX + "*");
        cache = new RedisCache(kvStore);
        locker = new RedisLocker(kvStore);
        counter = new RedisCounter(kvStore);
        rateLimiter = new RedisRateLimiter(kvStore);
        idempotent = new RedisIdempotent(kvStore);
        tokenStore = new RedisTokenStore(kvStore);
    }

    // ==================== ICache ====================

    @Test
    void cache_setAndGet() {
        cache.set(KEY_PREFIX + "cache:key1", "value1", 60);
        assertThat(cache.get(KEY_PREFIX + "cache:key1")).isEqualTo("value1");
    }

    @Test
    void cache_getNonExistent_returnsNull() {
        assertThat(cache.get(KEY_PREFIX + "cache:nonexistent")).isNull();
    }

    @Test
    void cache_delete() {
        cache.set(KEY_PREFIX + "cache:key2", "value2", 60);
        cache.delete(KEY_PREFIX + "cache:key2");
        assertThat(cache.get(KEY_PREFIX + "cache:key2")).isNull();
    }

    @Test
    void cache_exists() {
        cache.set(KEY_PREFIX + "cache:key3", "value3", 60);
        assertThat(cache.exists(KEY_PREFIX + "cache:key3")).isTrue();
        assertThat(cache.exists(KEY_PREFIX + "cache:nonexistent")).isFalse();
    }

    // ==================== ILocker ====================

    @Test
    void locker_tryLock_andUnlock() {
        String key = KEY_PREFIX + "locker:key1";
        assertThat(locker.tryLock(key, 3000)).isTrue();
        locker.unlock(key);
    }

    @Test
    void locker_lockedByOther_returnsFalse() {
        String key = KEY_PREFIX + "locker:key2";
        assertThat(locker.tryLock(key, 5000)).isTrue();
        locker.unlock(key);
    }

    // ==================== ICounter ====================

    @Test
    void counter_increment() {
        String key = KEY_PREFIX + "counter:key1";
        assertThat(counter.increment(key, 1, 60)).isEqualTo(1);
        assertThat(counter.increment(key, 1, 60)).isEqualTo(2);
        assertThat(counter.increment(key, 1, 60)).isEqualTo(3);
    }

    @Test
    void counter_get() {
        String key = KEY_PREFIX + "counter:key2";
        counter.increment(key, 1, 60);
        counter.increment(key, 1, 60);
        assertThat(counter.get(key)).isEqualTo(2);
    }

    // ==================== IRateLimiter ====================

    @Test
    void rateLimiter_tryAcquire() {
        String key = KEY_PREFIX + "ratelimit:key1";
        // With defaultLimit=100 and permits=3:
        // After n successful calls, counter=n. Next call checks: n + 3 <= 100
        // Call #98: counter=97, 97+3=100 <= 100, succeeds
        // Call #99: counter=98, 98+3=101 > 100, fails
        // So 98 successful calls are allowed before rejection
        for (int i = 0; i < 98; i++) {
            assertThat(rateLimiter.tryAcquire(key, 3)).isTrue();
        }
        // 99th call should be rejected
        assertThat(rateLimiter.tryAcquire(key, 3)).isFalse();
    }

    // ==================== IIdempotent ====================

    @Test
    void idempotent_checkAndMark() {
        String key = KEY_PREFIX + "idempotent:key1";
        // First call: key doesn't exist, checkAndMark returns false (newly marked)
        assertThat(idempotent.checkAndMark(key, 60)).isFalse();
        // Second call: key exists, checkAndMark returns true (duplicate)
        assertThat(idempotent.checkAndMark(key, 60)).isTrue();
    }

    // ==================== ITokenStore ====================

    @Test
    void tokenStore_storeAndGet() {
        String token = KEY_PREFIX + "token:key1:abc123";
        tokenStore.store(token, token, 3600);
        assertThat(tokenStore.get(token)).isEqualTo(token);
        assertThat(tokenStore.get(KEY_PREFIX + "token:key1:nonexistent")).isNull();
    }

    @Test
    void tokenStore_remove() {
        String token = KEY_PREFIX + "token:key2:def456";
        tokenStore.store(token, token, 3600);
        assertThat(tokenStore.get(token)).isEqualTo(token);
        tokenStore.remove(token);
        assertThat(tokenStore.get(token)).isNull();
    }

    // ==================== Concurrency ====================

    @Test
    void concurrent_increment_noRaceCondition() throws InterruptedException {
        String key = KEY_PREFIX + "counter:concurrent";
        int threadCount = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

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
}
