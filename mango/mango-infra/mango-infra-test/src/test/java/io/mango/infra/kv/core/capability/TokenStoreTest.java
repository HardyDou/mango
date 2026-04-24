package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.ITokenStore;
import io.mango.infra.kv.core.KvStoreTestFixtures.StoreFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenStoreTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void store_overwrite_remove_and_isolation(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ITokenStore tokenStore = new KvStoreTokenStore(fixture.store());

            String key = fixture.key("access:1");
            String otherKey = fixture.key("access:2");
            tokenStore.store(key, "v1", 60);
            tokenStore.store(key, "v2", 60);
            tokenStore.store(otherKey, "v3", 60);

            assertThat(tokenStore.get(key)).isEqualTo("v2");
            assertThat(tokenStore.get(otherKey)).isEqualTo("v3");

            tokenStore.remove(key);
            assertThat(tokenStore.get(key)).isNull();
            assertThat(tokenStore.get(otherKey)).isEqualTo("v3");
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

            assertThatThrownBy(() -> tokenStore.store(null, "v", 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tokenStore.store("  ", "v", 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tokenStore.store(key, null, 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tokenStore.store(key, "v", 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
