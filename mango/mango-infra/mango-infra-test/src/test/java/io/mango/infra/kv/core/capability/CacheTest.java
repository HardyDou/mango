package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.ICache;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void set_get_exists_delete(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICache cache = new KvStoreCache(fixture.store());

            String key = fixture.key("user:1");
            cache.set(key, "v1", 60);
            assertThat(cache.get(key)).isEqualTo("v1");
            assertThat(cache.exists(key)).isTrue();

            cache.delete(key);
            assertThat(cache.get(key)).isNull();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void set_overwritesExistingValue(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICache cache = new KvStoreCache(fixture.store());

            String key = fixture.key("user:1");
            cache.set(key, "v1", 60);
            cache.set(key, "v2", 60);

            assertThat(cache.get(key)).isEqualTo("v2");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void invalidArguments_throw(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICache cache = new KvStoreCache(fixture.store());

            assertThatThrownBy(() -> cache.set(null, "v", 60))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> cache.set("k", null, 60))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> cache.set("k", "v", 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void keys_areIsolated(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICache cache = new KvStoreCache(fixture.store());
            String key1 = fixture.key("user:1");
            String key2 = fixture.key("user:2");

            cache.set(key1, "v1", 60);
            cache.set(key2, "v2", 60);

            assertThat(cache.get(key1)).isEqualTo("v1");
            assertThat(cache.get(key2)).isEqualTo("v2");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void valueExpiresAfterTtl(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICache cache = new KvStoreCache(fixture.store());
            String key = fixture.key("user:ttl");

            cache.set(key, "v1", 1);
            assertThat(cache.get(key)).isEqualTo("v1");
            Thread.sleep(1200);

            assertThat(cache.get(key)).isNull();
            assertThat(cache.exists(key)).isFalse();
        }
    }
}
