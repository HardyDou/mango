package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.ILocker;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KvStoreLocker implements ILocker {

    private final IKvStore kvStore;

    @Override
    public boolean tryLock(String key, long ttlSeconds) {
        validateKey(key);
        validateTtl(ttlSeconds);
        return kvStore.setIfAbsent(key, "1", ttlSeconds);
    }

    @Override
    public void unlock(String key) {
        validateKey(key);
        kvStore.delete(key);
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }

    private void validateTtl(long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be positive");
        }
    }
}
