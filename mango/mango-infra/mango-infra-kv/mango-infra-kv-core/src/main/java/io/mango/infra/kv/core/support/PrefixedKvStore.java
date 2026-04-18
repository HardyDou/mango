package io.mango.infra.kv.core.support;

import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

/**
 * IKvStore decorator that applies one KV capability namespace.
 */
@RequiredArgsConstructor
public class PrefixedKvStore implements IKvStore {

    private final IKvStore delegate;
    private final KvKeyNormalizer keyNormalizer;
    private final String capability;

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        return delegate.put(normalize(key), value, expireSeconds);
    }

    @Override
    public boolean setIfAbsent(String key, String value, long expireSeconds) {
        return delegate.setIfAbsent(normalize(key), value, expireSeconds);
    }

    @Override
    public void set(String key, String value, long expireSeconds) {
        delegate.set(normalize(key), value, expireSeconds);
    }

    @Override
    public String get(String key) {
        return delegate.get(normalize(key));
    }

    @Override
    public long increment(String key, long windowSeconds) {
        return delegate.increment(normalize(key), windowSeconds);
    }

    @Override
    public long incrementBy(String key, long delta, long windowSeconds) {
        return delegate.incrementBy(normalize(key), delta, windowSeconds);
    }

    @Override
    public void delete(String key) {
        delegate.delete(normalize(key));
    }

    @Override
    public boolean exists(String key) {
        return delegate.exists(normalize(key));
    }

    private String normalize(String key) {
        return keyNormalizer.normalize(capability, key);
    }
}
