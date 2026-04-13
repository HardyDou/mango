package io.mango.kv.core;

import io.mango.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collections;

/**
 * RedisKvStore implementation using Redisson.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisKvStore implements IKvStore {

    private final RedissonClient redissonClient;

    /**
     * Lua script: atomically increment counter and set TTL on first call.
     * KEYS[1] = atomic long key
     * ARGV[1] = TTL in seconds
     * Returns: current count after increment
     */
    private static final String INCREMENT_WITH_TTL_SCRIPT =
        "local count = redis.call('incr', KEYS[1]) "
      + "if count == 1 then redis.call('expire', KEYS[1], ARGV[1]) end "
      + "return count";

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            return putNonPositiveTtl(key);
        }
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent(value, Duration.ofSeconds(expireSeconds));
    }

    @Override
    public String get(String key) {
        validateKey(key);
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    public long increment(String key, long windowSeconds) {
        validateKey(key);
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive, was: " + windowSeconds);
        }
        RScript script = redissonClient.getScript();
        Long count = (Long) script.eval(
            RScript.Mode.READ_WRITE,
            INCREMENT_WITH_TTL_SCRIPT,
            RScript.ReturnType.INTEGER,
            Collections.singletonList(key),
            String.valueOf(windowSeconds)
        );
        return count != null ? count : 0;
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        redissonClient.getBucket(key).delete();
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        return redissonClient.getBucket(key).isExists();
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }
}
