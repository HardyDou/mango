package io.mango.kv.redis;

import io.mango.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis KV store implementation using SETNX semantics.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisKvStore implements IKvStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        validateKey(key);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value,
            Duration.ofSeconds(expireSeconds));
        return Boolean.TRUE.equals(result);
    }

    @Override
    public String get(String key) {
        validateKey(key);
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public long increment(String key, long windowSeconds) {
        validateKey(key);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        return count != null ? count : 0;
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        redisTemplate.delete(key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }
}
