package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.ICache;
import io.mango.infra.kv.core.KvStoreTestFixtures.StoreFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void set_overwrite_delete_and_isolation(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICache cache = new KvStoreCache(fixture.store());

            String key = fixture.key("user:1");
            String otherKey = fixture.key("user:2");
            cache.set(key, "v1", 60);
            cache.set(key, "v2", 60);
            cache.set(otherKey, "v3", 60);

            assertThat(cache.get(key)).isEqualTo("v2");
            assertThat(cache.get(otherKey)).isEqualTo("v3");
            assertThat(cache.exists(key)).isTrue();

            cache.delete(key);
            assertThat(cache.get(key)).isNull();
            assertThat(cache.get(otherKey)).isEqualTo("v3");
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void invalidArguments_throw(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICache cache = new KvStoreCache(fixture.store());

            assertThatThrownBy(() -> cache.set(null, "v", 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> cache.set("k", null, 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> cache.set("k", "v", 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
