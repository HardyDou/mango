package io.mango.infra.kv.core.redis;

import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

/**
 * RedisKvStore implementation using Redisson.
 *
 * Uses RAtomicLong for increment operations (atomic counter).
 * Uses RBucket for string storage (put/get).
 *
 * Note: Due to Redis type system, a key can only be one type.
 * Keys used with increment() should only be accessed via increment/get operations.
 * Keys used with put() should only be accessed via put/get operations.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisKvStore implements IKvStore {

    private final RedissonClient redissonClient;

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
        RAtomicLong atomic = redissonClient.getAtomicLong(key);

        // Try bucket first (string values from put)
        try {
            return bucket.get();
        } catch (Exception e) {
            // bucket.get() failed - key might not exist or is not STRING type
        }

        // Try atomic (counter values from increment)
        try {
            return String.valueOf(atomic.get());
        } catch (Exception e) {
            // atomic.get() failed - key might not exist or is not LONG type
            return null;
        }
    }

    @Override
    public long increment(String key, long windowSeconds) {
        validateKey(key);
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive, was: " + windowSeconds);
        }
        RAtomicLong atomic = redissonClient.getAtomicLong(key);
        long count = atomic.incrementAndGet();
        // Set TTL on first increment (when count == 1)
        // Use expireAsync to avoid blocking
        if (count == 1) {
            atomic.expire(Duration.ofSeconds(windowSeconds));
        }
        return count;
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        // Delete both bucket and atomic - one of them may have the key
        redissonClient.getBucket(key).delete();
        redissonClient.getAtomicLong(key).delete();
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        RBucket<String> bucket = redissonClient.getBucket(key);
        RAtomicLong atomic = redissonClient.getAtomicLong(key);

        // Try bucket (string values from put)
        try {
            if (bucket.isExists()) {
                return true;
            }
        } catch (Exception e) {
            // bucket.isExists() may throw if key is not STRING type
        }

        // Try atomic (counter values from increment)
        try {
            return atomic.isExists();
        } catch (Exception e) {
            return false;
        }
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }
}
