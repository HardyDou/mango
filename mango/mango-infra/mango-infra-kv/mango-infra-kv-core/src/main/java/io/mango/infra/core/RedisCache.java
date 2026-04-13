package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

/**
 * Redis implementation of ICache using IKvStore.
 */
@RequiredArgsConstructor
public class RedisCache implements ICache {

    private final IKvStore kvStore;

    @Override
    public void set(String key, String value, long ttlSeconds) {
        validateKey(key);
        validateValue(value);
        validateTtl(ttlSeconds);
        kvStore.put(key, value, ttlSeconds);
    }

    @Override
    public String get(String key) {
        validateKey(key);
        return kvStore.get(key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        return kvStore.exists(key);
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        kvStore.delete(key);
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }

    private void validateValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
    }

    private void validateTtl(long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be positive");
        }
    }
}
