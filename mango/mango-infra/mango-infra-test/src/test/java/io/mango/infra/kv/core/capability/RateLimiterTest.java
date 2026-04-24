package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.core.KvStoreTestFixtures.StoreFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void tryAcquire_enforcesLimitAndKeepsKeysIsolated(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            KvStoreRateLimiter limiter = new KvStoreRateLimiter(fixture.store());
            String key = fixture.key("login:ip:1");
            String otherKey = fixture.key("login:ip:2");

            assertThat(limiter.tryAcquire(key, 1, 3, 60)).isTrue();
            assertThat(limiter.tryAcquire(key, 2, 3, 60)).isTrue();
            assertThat(limiter.tryAcquire(key, 1, 3, 60)).isFalse();

            assertThat(limiter.tryAcquire(otherKey, 3, 3, 60)).isTrue();
            assertThat(limiter.tryAcquire(otherKey, 1, 3, 60)).isFalse();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void windowExpirationAllowsAgain(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            KvStoreRateLimiter limiter = new KvStoreRateLimiter(fixture.store());
            String key = fixture.key("login:ip:ttl");

            assertThat(limiter.tryAcquire(key, 3, 3, 1)).isTrue();
            assertThat(limiter.tryAcquire(key, 1, 3, 1)).isFalse();
            Thread.sleep(1200);

            assertThat(limiter.tryAcquire(key, 3, 3, 60)).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void invalidArguments_throw(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            KvStoreRateLimiter limiter = new KvStoreRateLimiter(fixture.store());
            String key = fixture.key("login:ip:invalid");

            assertThatThrownBy(() -> limiter.tryAcquire(null, 1, 3, 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> limiter.tryAcquire("  ", 1, 3, 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> limiter.tryAcquire(key, 0, 3, 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> limiter.tryAcquire(key, 1, 0, 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> limiter.tryAcquire(key, 1, 3, 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
