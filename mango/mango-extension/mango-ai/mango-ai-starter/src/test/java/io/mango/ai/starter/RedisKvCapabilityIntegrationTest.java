package io.mango.ai.starter;

import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.core.capability.KvStoreCache;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis-backed Mango KV capability integration test.
 */
@Tag("integration")
@Tag("ai")
class RedisKvCapabilityIntegrationTest {

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
    void redisCapabilities_preserveConversationTtlAndRateLimit() {
        Assumptions.assumeTrue(redisson != null, "local Redis is required for this integration test");

        RedisKvStore store = new RedisKvStore(redisson);
        ICache cache = new KvStoreCache(store);
        KvStoreRateLimiter rateLimiter = new KvStoreRateLimiter(store);
        String suffix = UUID.randomUUID().toString();
        String conversationKey = "mango-ai:test:conversation:" + suffix;
        String rateLimitKey = "mango-ai:test:rate-limit:" + suffix;
        try {
            cache.set(conversationKey, "[\"user\",\"assistant\"]", 30);
            assertEquals("[\"user\",\"assistant\"]", cache.get(conversationKey));
            assertTrue(redisson.getBucket(conversationKey).remainTimeToLive() > 0);

            assertTrue(rateLimiter.tryAcquire(rateLimitKey, 1, 2, 30));
            assertTrue(rateLimiter.tryAcquire(rateLimitKey, 1, 2, 30));
            assertFalse(rateLimiter.tryAcquire(rateLimitKey, 1, 2, 30));
        } finally {
            cache.delete(conversationKey);
            store.delete(rateLimitKey);
        }
    }
}
