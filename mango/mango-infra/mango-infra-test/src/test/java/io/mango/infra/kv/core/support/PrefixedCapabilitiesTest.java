package io.mango.infra.kv.core.support;

import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.api.ICounter;
import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.api.IRateLimiter;
import io.mango.infra.kv.api.ITokenStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrefixedCapabilitiesTest {

    private final KvKeyNormalizer normalizer = new KvKeyNormalizer(true, "mango:kv", "prod", false, null);

    @Test
    void cache_addsCacheNamespace() {
        RecordingCache delegate = new RecordingCache();
        ICache cache = new PrefixedCapabilities.Cache(delegate, normalizer);

        cache.set("user:1", "value", 60);

        assertThat(delegate.lastKey).isEqualTo("mango:kv:prod:cache:user:1");
    }

    @Test
    void locker_addsLockNamespace() {
        RecordingLocker delegate = new RecordingLocker();
        ILocker locker = new PrefixedCapabilities.Locker(delegate, normalizer);

        locker.tryLock("order:1", 60);

        assertThat(delegate.lastKey).isEqualTo("mango:kv:prod:lock:order:1");
    }

    @Test
    void counter_addsCounterNamespace() {
        RecordingCounter delegate = new RecordingCounter();
        ICounter counter = new PrefixedCapabilities.Counter(delegate, normalizer);

        counter.increment("sms:18800000000", 1, 60);

        assertThat(delegate.lastKey).isEqualTo("mango:kv:prod:counter:sms:18800000000");
    }

    @Test
    void rateLimiter_addsRateLimitNamespace() {
        RecordingRateLimiter delegate = new RecordingRateLimiter();
        IRateLimiter rateLimiter = new PrefixedCapabilities.RateLimiter(delegate, normalizer);

        rateLimiter.tryAcquire("login:ip:127.0.0.1", 1);

        assertThat(delegate.lastKey).isEqualTo("mango:kv:prod:rate-limit:login:ip:127.0.0.1");
    }

    @Test
    void idempotent_addsIdempotentNamespace() {
        RecordingIdempotent delegate = new RecordingIdempotent();
        IIdempotent idempotent = new PrefixedCapabilities.Idempotent(delegate, normalizer);

        idempotent.checkAndMark("payment:req-abc", 60);

        assertThat(delegate.lastKey).isEqualTo("mango:kv:prod:idempotent:payment:req-abc");
    }

    @Test
    void tokenStore_addsTokenNamespace() {
        RecordingTokenStore delegate = new RecordingTokenStore();
        ITokenStore tokenStore = new PrefixedCapabilities.TokenStore(delegate, normalizer);

        tokenStore.store("access:sha256", "value", 60);

        assertThat(delegate.lastKey).isEqualTo("mango:kv:prod:token:access:sha256");
    }

    private abstract static class Recording {
        String lastKey;
    }

    private static class RecordingCache extends Recording implements ICache {
        @Override
        public void set(String key, String value, long ttlSeconds) {
            lastKey = key;
        }

        @Override
        public String get(String key) {
            lastKey = key;
            return null;
        }

        @Override
        public boolean exists(String key) {
            lastKey = key;
            return false;
        }

        @Override
        public void delete(String key) {
            lastKey = key;
        }
    }

    private static class RecordingLocker extends Recording implements ILocker {
        @Override
        public boolean tryLock(String key, long ttlSeconds) {
            lastKey = key;
            return true;
        }

        @Override
        public void unlock(String key) {
            lastKey = key;
        }
    }

    private static class RecordingCounter extends Recording implements ICounter {
        @Override
        public long increment(String key, long delta, long windowSeconds) {
            lastKey = key;
            return 1;
        }

        @Override
        public long get(String key) {
            lastKey = key;
            return 0;
        }
    }

    private static class RecordingRateLimiter extends Recording implements IRateLimiter {
        @Override
        public boolean tryAcquire(String key, int permits) {
            lastKey = key;
            return true;
        }
    }

    private static class RecordingIdempotent extends Recording implements IIdempotent {
        @Override
        public boolean isDuplicate(String key, long windowSeconds) {
            lastKey = key;
            return false;
        }

        @Override
        public void mark(String key, long windowSeconds) {
            lastKey = key;
        }

        @Override
        public boolean checkAndMark(String key, long windowSeconds) {
            lastKey = key;
            return false;
        }
    }

    private static class RecordingTokenStore extends Recording implements ITokenStore {
        @Override
        public void store(String token, String value, long ttlSeconds) {
            lastKey = token;
        }

        @Override
        public String get(String token) {
            lastKey = token;
            return null;
        }

        @Override
        public void remove(String token) {
            lastKey = token;
        }
    }
}
