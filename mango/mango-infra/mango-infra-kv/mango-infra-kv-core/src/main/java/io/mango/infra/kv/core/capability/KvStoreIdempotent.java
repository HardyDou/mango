package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KvStoreIdempotent implements IIdempotent {

    private final IKvStore kvStore;

    @Override
    public boolean isDuplicate(String key, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        return kvStore.exists(key);
    }

    @Override
    public void mark(String key, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        kvStore.setIfAbsent(key, "1", windowSeconds);
    }

    @Override
    public boolean checkAndMark(String key, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        return !kvStore.setIfAbsent(key, "1", windowSeconds);
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }

    private void validateWindow(long windowSeconds) {
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
    }
}
