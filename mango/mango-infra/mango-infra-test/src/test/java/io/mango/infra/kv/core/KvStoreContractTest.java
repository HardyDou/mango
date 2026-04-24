package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IKvStore;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KvStoreContractTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.mango.infra.kv.core.KvStoreTestFixtures#kvStores")
    void setIfAbsent_existingKey_rejectsOverwrite(String name, KvStoreTestFixtures.StoreFixture fixture) throws Exception {
        try (fixture) {
            IKvStore store = fixture.namespacedStore();

            assertThat(store.setIfAbsent("user:1", "v1", 60)).isTrue();
            assertThat(store.setIfAbsent("user:1", "v2", 60)).isFalse();
            assertThat(store.get("user:1")).isEqualTo("v1");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.mango.infra.kv.core.KvStoreTestFixtures#kvStores")
    void set_existingKey_overwritesValue(String name, KvStoreTestFixtures.StoreFixture fixture) throws Exception {
        try (fixture) {
            IKvStore store = fixture.namespacedStore();

            store.setIfAbsent("cache:user:1", "v1", 60);
            store.set("cache:user:1", "v2", 60);

            assertThat(store.get("cache:user:1")).isEqualTo("v2");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.mango.infra.kv.core.KvStoreTestFixtures#kvStores")
    void ttl_expiredEntry_becomesInvisible(String name, KvStoreTestFixtures.StoreFixture fixture) throws Exception {
        try (fixture) {
            IKvStore store = fixture.namespacedStore();

            store.set("token:1", "abc", 1);
            Thread.sleep(1200);

            assertThat(store.get("token:1")).isNull();
            assertThat(store.exists("token:1")).isFalse();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.mango.infra.kv.core.KvStoreTestFixtures#kvStores")
    void increment_differentKeys_areIsolated(String name, KvStoreTestFixtures.StoreFixture fixture) throws Exception {
        try (fixture) {
            IKvStore store = fixture.namespacedStore();

            assertThat(store.incrementBy("counter:a", 2, 60)).isEqualTo(2);
            assertThat(store.incrementBy("counter:b", 1, 60)).isEqualTo(1);
            assertThat(store.incrementBy("counter:a", 3, 60)).isEqualTo(5);
            assertThat(store.get("counter:b")).isEqualTo("1");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.mango.infra.kv.core.KvStoreTestFixtures#kvStores")
    void invalidKey_throwsIllegalArgumentException(String name, KvStoreTestFixtures.StoreFixture fixture) throws Exception {
        try (fixture) {
            IKvStore store = fixture.rawStore();

            assertThrows(IllegalArgumentException.class, () -> store.get(" "));
            assertThrows(IllegalArgumentException.class, () -> store.set(null, "v", 60));
            assertThrows(IllegalArgumentException.class, () -> store.delete(""));
        }
    }
}
