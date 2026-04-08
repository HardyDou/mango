package io.mango.dal.core;

import io.mango.dal.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * RedisXivStore implementation using Redisson.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisXivStore implements IKvStore {

    private final RedissonClient redissonClient;

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        validateKey(key);
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
        // Redisson's increment doesn't support TTL directly, so we set it separately
        long count = redissonClient.getAtomicLong(key).incrementAndGet();
        if (count == 1) {
            redissonClient.getAtomicLong(key).expire(windowSeconds, TimeUnit.SECONDS);
        }
        return count;
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
