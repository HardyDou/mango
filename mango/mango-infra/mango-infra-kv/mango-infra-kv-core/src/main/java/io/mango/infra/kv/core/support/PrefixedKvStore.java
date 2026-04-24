package io.mango.infra.kv.core.support;

import io.mango.infra.kv.api.IKvSortedSet;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

/**
 * IKvStore decorator that applies one KV capability namespace.
 */
@RequiredArgsConstructor
public class PrefixedKvStore implements IKvStore, IKvSortedSet {

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

    @Override
    public void add(String key, String member, double score, long ttlSeconds) {
        if (delegate instanceof IKvSortedSet sortedSet) {
            sortedSet.add(normalize(key), member, score, ttlSeconds);
        }
    }

    @Override
    public void remove(String key, String member) {
        if (delegate instanceof IKvSortedSet sortedSet) {
            sortedSet.remove(normalize(key), member);
        }
    }

    @Override
    public Collection<String> rangeByScore(String key, double minScore, double maxScore, int limit) {
        if (delegate instanceof IKvSortedSet sortedSet) {
            return sortedSet.rangeByScore(normalize(key), minScore, maxScore, limit);
        }
        return java.util.List.of();
    }

    @Override
    public long removeByScore(String key, double minScore, double maxScore) {
        if (delegate instanceof IKvSortedSet sortedSet) {
            return sortedSet.removeByScore(normalize(key), minScore, maxScore);
        }
        return 0;
    }

    @Override
    public long size(String key) {
        if (delegate instanceof IKvSortedSet sortedSet) {
            return sortedSet.size(normalize(key));
        }
        return 0;
    }

    private String normalize(String key) {
        return keyNormalizer.normalize(capability, key);
    }
}
