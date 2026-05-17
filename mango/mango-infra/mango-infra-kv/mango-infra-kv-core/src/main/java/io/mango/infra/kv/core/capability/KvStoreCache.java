package io.mango.infra.kv.core.capability;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KvStoreCache implements ICache {

    private final IKvStore kvStore;

    @Override
    public void set(String key, String value, long ttlSeconds) {
        validateKey(key);
        validateValue(value);
        validateTtl(ttlSeconds);
        kvStore.set(key, value, ttlSeconds);
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
        Require.notBlank(key, "key cannot be null or blank");
    }

    private void validateValue(String value) {
        Require.notNull(value, "value cannot be null");
    }

    private void validateTtl(long ttlSeconds) {
        Require.positive(ttlSeconds, "ttlSeconds must be positive");
    }
}
