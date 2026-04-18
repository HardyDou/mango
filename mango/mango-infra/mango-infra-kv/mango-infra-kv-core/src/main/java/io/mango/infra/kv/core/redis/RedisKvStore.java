package io.mango.infra.kv.core.redis;

import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.codec.StringCodec;
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
    public boolean setIfAbsent(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            return putNonPositiveTtl(key);
        }
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.setIfAbsent(value, Duration.ofSeconds(expireSeconds));
    }

    @Override
    public void set(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            putNonPositiveTtl(key);
            return;
        }
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(value, Duration.ofSeconds(expireSeconds));
    }

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        return setIfAbsent(key, value, expireSeconds);
    }

    @Override
    public String get(String key) {
        validateKey(key);
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.get();
    }

    @Override
    public long incrementBy(String key, long delta, long windowSeconds) {
        validateKey(key);
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive, was: " + windowSeconds);
        }
        RAtomicLong atomic = redissonClient.getAtomicLong(key);
        long count = atomic.addAndGet(delta);
        // Set TTL on first increment of a new key.
        // Use expireAsync to avoid blocking
        if (count == delta) {
            atomic.expire(Duration.ofSeconds(windowSeconds));
        }
        return count;
    }

    @Override
    public long increment(String key, long windowSeconds) {
        return incrementBy(key, 1, windowSeconds);
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        redissonClient.getKeys().delete(key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        return redissonClient.getKeys().countExists(key) > 0;
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }
}
