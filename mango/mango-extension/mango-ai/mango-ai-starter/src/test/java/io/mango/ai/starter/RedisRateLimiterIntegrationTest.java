package io.mango.ai.starter;

import io.mango.infra.kv.core.capability.KvStoreRateLimiter;
import io.mango.infra.kv.core.redis.RedisKvStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Redis 限流能力集成测试。 */
@Tag("integration")
@Tag("ai")
class RedisRateLimiterIntegrationTest {

    private static RedissonClient redisson;

    @BeforeAll
    static void connectRedis() {
        try {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://127.0.0.1:6379");
            redisson = Redisson.create(config);
            redisson.getKeys().countExists("mango-ai-redis-connectivity");
        } catch (RuntimeException exception) {
            redisson = null;
        }
    }

    @AfterAll
    static void closeRedis() {
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    @Test
    void redisRateLimiterEnforcesConfiguredPermitLimit() {
        Assumptions.assumeTrue(redisson != null, "local Redis is required for this integration test");

        RedisKvStore store = new RedisKvStore(redisson);
        KvStoreRateLimiter rateLimiter = new KvStoreRateLimiter(store);
        String rateLimitKey = "mango-ai:test:rate-limit:" + UUID.randomUUID();
        try {
            assertTrue(rateLimiter.tryAcquire(rateLimitKey, 1, 2, 30));
            assertTrue(rateLimiter.tryAcquire(rateLimitKey, 1, 2, 30));
            assertFalse(rateLimiter.tryAcquire(rateLimitKey, 1, 2, 30));
        } finally {
            store.delete(rateLimitKey);
        }
    }
}
