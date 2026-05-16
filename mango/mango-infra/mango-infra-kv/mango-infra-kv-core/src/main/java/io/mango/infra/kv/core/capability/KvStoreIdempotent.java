package io.mango.infra.kv.core.capability;

import io.mango.common.result.Require;
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
        Require.notBlank(key, "key cannot be null or blank");
    }

    private void validateWindow(long windowSeconds) {
        Require.positive(windowSeconds, "windowSeconds must be positive");
    }
}
