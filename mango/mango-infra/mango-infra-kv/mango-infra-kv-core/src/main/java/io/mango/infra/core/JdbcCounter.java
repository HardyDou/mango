package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ICounter;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

/**
 * JDBC implementation of ICounter using IKvStore.
 * Note: IKvStore.increment only supports +1, so delta is always 1.
 * For arbitrary delta support, use SQL atomic commands directly.
 */
@RequiredArgsConstructor
public class JdbcCounter implements ICounter {

    private final IKvStore kvStore;

    @Override
    public long increment(String key, long delta, long windowSeconds) {
        validateKey(key);
        validateWindow(windowSeconds);
        // IKvStore.increment only supports +1, call multiple times for delta > 1
        long result = kvStore.increment(key, windowSeconds);
        for (long i = 1; i < delta; i++) {
            kvStore.increment(key, windowSeconds);
        }
        return result + delta - 1;
    }

    @Override
    public long get(String key) {
        validateKey(key);
        String value = kvStore.get(key);
        return value == null ? 0L : Long.parseLong(value);
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
