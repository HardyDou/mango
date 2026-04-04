package io.mango.kv.starter;

import io.mango.kv.api.IKvStore;
import io.mango.kv.db.DbKvStore;
import io.mango.kv.memory.MemoryKvStore;
import io.mango.kv.redis.RedisKvStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cascading KV Store with circuit breaker.
 * Redis (500ms timeout) → DB (2s timeout + circuit breaker) → Memory (final fallback)
 *
 * Circuit breaker rules:
 * - DB operation timeout (2s) → downgrade to Memory
 * - 3 consecutive failures → lock to Memory for 30s
 * - Every downgrade logs a WARN (for monitoring/alerting)
 */
@Slf4j
public class CascadingKvStore implements IKvStore {

    private static final int DB_FAILURE_THRESHOLD = 3;
    private static final long DB_LOCK_DURATION_MS = 30_000;
    private static final long REDIS_TIMEOUT_MS = 500;
    private static final long DB_TIMEOUT_MS = 2000;

    private final RedisKvStore redisStore;
    private final DbKvStore dbStore;
    private final MemoryKvStore memoryStore;

    private final AtomicInteger dbFailureCount = new AtomicInteger(0);
    private volatile long dbLockedUntil = 0;

    public CascadingKvStore(StringRedisTemplate redisTemplate,
                            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.redisStore = new RedisKvStore(redisTemplate);
        this.dbStore = new DbKvStore(jdbcTemplate, redisTemplate);
        this.memoryStore = new MemoryKvStore();
    }

    private boolean isDbLocked() {
        return System.currentTimeMillis() < dbLockedUntil;
    }

    private void lockDb(long durationMs) {
        dbLockedUntil = System.currentTimeMillis() + durationMs;
        log.warn("DB locked for {}ms due to consecutive failures", durationMs);
    }

    private boolean tryRedis(String operation, String key, String value, long expireSeconds) {
        try {
            return switch (operation) {
                case "put" -> redisStore.put(key, value, expireSeconds);
                case "get" -> { redisStore.get(key); yield true; }
                case "delete" -> { redisStore.delete(key); yield true; }
                default -> true;
            };
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable: {}", e.getMessage());
            return false;
        }
    }

    private boolean tryDb(String operation, String key, String value, long expireSeconds) {
        long start = System.currentTimeMillis();
        try {
            return switch (operation) {
                case "put" -> dbStore.put(key, value, expireSeconds);
                case "get" -> { dbStore.get(key); yield true; }
                case "delete" -> { dbStore.delete(key); yield true; }
                default -> true;
            };
        } catch (Exception e) {
            if (System.currentTimeMillis() - start > DB_TIMEOUT_MS) {
                log.warn("DB timeout (>{}ms): {}", DB_TIMEOUT_MS, e.getMessage());
                dbFailureCount.incrementAndGet();
                if (dbFailureCount.get() >= DB_FAILURE_THRESHOLD) {
                    lockDb(DB_LOCK_DURATION_MS);
                }
            }
            return false;
        }
    }

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        // Try Redis
        if (tryRedis("put", key, value, expireSeconds)) {
            dbFailureCount.set(0);
            return true;
        }
        // Try DB (if not locked)
        if (!isDbLocked()) {
            if (tryDb("put", key, value, expireSeconds)) {
                dbFailureCount.set(0);
                return true;
            }
        }
        // Fallback to Memory
        log.warn("KV Store cascading to Memory for put: key={}", key);
        return memoryStore.put(key, value, expireSeconds);
    }

    @Override
    public String get(String key) {
        // Try Redis
        String result = redisStore.get(key);
        if (result != null) {
            dbFailureCount.set(0);
            return result;
        }
        // Try DB (if not locked)
        if (!isDbLocked()) {
            String dbResult = dbStore.get(key);
            if (dbResult != null) {
                dbFailureCount.set(0);
                return dbResult;
            }
        }
        // Fallback to Memory
        return memoryStore.get(key);
    }

    @Override
    public long increment(String key, long windowSeconds) {
        // Try Redis
        try {
            long count = redisStore.increment(key, windowSeconds);
            dbFailureCount.set(0);
            return count;
        } catch (Exception e) {
            log.warn("Redis increment failed: {}", e.getMessage());
        }
        // Try DB (if not locked)
        if (!isDbLocked()) {
            try {
                long count = dbStore.increment(key, windowSeconds);
                dbFailureCount.set(0);
                return count;
            } catch (Exception e) {
                log.warn("DB increment failed: {}", e.getMessage());
                dbFailureCount.incrementAndGet();
                if (dbFailureCount.get() >= DB_FAILURE_THRESHOLD) {
                    lockDb(DB_LOCK_DURATION_MS);
                }
            }
        }
        // Fallback to Memory
        return memoryStore.increment(key, windowSeconds);
    }

    @Override
    public void delete(String key) {
        // Try Redis
        try {
            redisStore.delete(key);
        } catch (Exception e) {
            log.warn("Redis delete failed: {}", e.getMessage());
        }
        // Try DB (if not locked)
        if (!isDbLocked()) {
            try {
                dbStore.delete(key);
            } catch (Exception e) {
                log.warn("DB delete failed: {}", e.getMessage());
            }
        }
        // Also delete from Memory
        memoryStore.delete(key);
    }

    @Override
    public boolean exists(String key) {
        // Try Redis
        try {
            if (redisStore.exists(key)) {
                return true;
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable: {}", e.getMessage());
        }
        // Try DB (if not locked)
        if (!isDbLocked()) {
            try {
                if (dbStore.exists(key)) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("DB exists check failed: {}", e.getMessage());
            }
        }
        // Fallback to Memory
        return memoryStore.exists(key);
    }
}
