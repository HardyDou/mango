package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.ITokenStore;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenStoreTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void store_get_remove(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ITokenStore tokenStore = new KvStoreTokenStore(fixture.store());

            String key = fixture.key("access:1");
            tokenStore.store(key, "v1", 60);
            assertThat(tokenStore.get(key)).isEqualTo("v1");

            tokenStore.remove(key);
            assertThat(tokenStore.get(key)).isNull();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void store_overwritesExistingValue(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ITokenStore tokenStore = new KvStoreTokenStore(fixture.store());

            String key = fixture.key("access:1");
            tokenStore.store(key, "v1", 60);
            tokenStore.store(key, "v2", 60);

            assertThat(tokenStore.get(key)).isEqualTo("v2");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void keys_areIsolated(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ITokenStore tokenStore = new KvStoreTokenStore(fixture.store());
            String key1 = fixture.key("access:1");
            String key2 = fixture.key("access:2");

            tokenStore.store(key1, "v1", 60);
            tokenStore.store(key2, "v2", 60);

            assertThat(tokenStore.get(key1)).isEqualTo("v1");
            assertThat(tokenStore.get(key2)).isEqualTo("v2");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void tokenExpiresAfterTtl(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ITokenStore tokenStore = new KvStoreTokenStore(fixture.store());
            String key = fixture.key("access:ttl");

            tokenStore.store(key, "v1", 1);
            assertThat(tokenStore.get(key)).isEqualTo("v1");
            Thread.sleep(1200);

            assertThat(tokenStore.get(key)).isNull();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void invalidArguments_throw(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ITokenStore tokenStore = new KvStoreTokenStore(fixture.store());
            String key = fixture.key("access:invalid");

            assertThatThrownBy(() -> tokenStore.store(null, "v", 60))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tokenStore.store("  ", "v", 60))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tokenStore.store(key, null, 60))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tokenStore.store(key, "v", 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
