package io.mango.infra.kv.core.support;

import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.ICounter;
import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.infra.kv.api.ITokenStore;
import lombok.RequiredArgsConstructor;

/**
 * Capability decorators that apply the Mango KV key namespace.
 */
public final class PrefixedCapabilities {

    private PrefixedCapabilities() {
    }

    @RequiredArgsConstructor
    public static class Cache implements ICache {
        private final ICache delegate;
        private final KvKeyNormalizer keyNormalizer;

        @Override
        public void set(String key, String value, long ttlSeconds) {
            delegate.set(keyNormalizer.normalize(KvKeyNormalizer.CACHE, key), value, ttlSeconds);
        }

        @Override
        public String get(String key) {
            return delegate.get(keyNormalizer.normalize(KvKeyNormalizer.CACHE, key));
        }

        @Override
        public boolean exists(String key) {
            return delegate.exists(keyNormalizer.normalize(KvKeyNormalizer.CACHE, key));
        }

        @Override
        public void delete(String key) {
            delegate.delete(keyNormalizer.normalize(KvKeyNormalizer.CACHE, key));
        }
    }

    @RequiredArgsConstructor
    public static class Locker implements ILocker {
        private final ILocker delegate;
        private final KvKeyNormalizer keyNormalizer;

        @Override
        public boolean tryLock(String key, long ttlSeconds) {
            return delegate.tryLock(keyNormalizer.normalize(KvKeyNormalizer.LOCK, key), ttlSeconds);
        }

        @Override
        public void unlock(String key) {
            delegate.unlock(keyNormalizer.normalize(KvKeyNormalizer.LOCK, key));
        }
    }

    @RequiredArgsConstructor
    public static class Counter implements ICounter {
        private final ICounter delegate;
        private final KvKeyNormalizer keyNormalizer;

        @Override
        public long increment(String key, long delta, long windowSeconds) {
            return delegate.increment(keyNormalizer.normalize(KvKeyNormalizer.COUNTER, key), delta, windowSeconds);
        }

        @Override
        public long get(String key) {
            return delegate.get(keyNormalizer.normalize(KvKeyNormalizer.COUNTER, key));
        }
    }

    @RequiredArgsConstructor
    public static class RateLimiter implements IRateLimiter {
        private final IRateLimiter delegate;
        private final KvKeyNormalizer keyNormalizer;

        @Override
        public boolean tryAcquire(String key, int permits) {
            return delegate.tryAcquire(keyNormalizer.normalize(KvKeyNormalizer.RATE_LIMIT, key), permits);
        }
    }

    @RequiredArgsConstructor
    public static class Idempotent implements IIdempotent {
        private final IIdempotent delegate;
        private final KvKeyNormalizer keyNormalizer;

        @Override
        public boolean isDuplicate(String key, long windowSeconds) {
            return delegate.isDuplicate(keyNormalizer.normalize(KvKeyNormalizer.IDEMPOTENT, key), windowSeconds);
        }

        @Override
        public void mark(String key, long windowSeconds) {
            delegate.mark(keyNormalizer.normalize(KvKeyNormalizer.IDEMPOTENT, key), windowSeconds);
        }

        @Override
        public boolean checkAndMark(String key, long windowSeconds) {
            return delegate.checkAndMark(keyNormalizer.normalize(KvKeyNormalizer.IDEMPOTENT, key), windowSeconds);
        }
    }

    @RequiredArgsConstructor
    public static class TokenStore implements ITokenStore {
        private final ITokenStore delegate;
        private final KvKeyNormalizer keyNormalizer;

        @Override
        public void store(String token, String value, long ttlSeconds) {
            delegate.store(keyNormalizer.normalize(KvKeyNormalizer.TOKEN, token), value, ttlSeconds);
        }

        @Override
        public String get(String token) {
            return delegate.get(keyNormalizer.normalize(KvKeyNormalizer.TOKEN, token));
        }

        @Override
        public void remove(String token) {
            delegate.remove(keyNormalizer.normalize(KvKeyNormalizer.TOKEN, token));
        }
    }
}
