package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.IIdempotent;
import io.mango.infra.kv.core.KvStoreTestFixtures.StoreFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotentTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void checkAndMark_marksDuplicateAndKeepsKeysIsolated(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            IIdempotent idempotent = new KvStoreIdempotent(fixture.store());
            String key = fixture.key("request:1");
            String otherKey = fixture.key("request:2");

            assertThat(idempotent.checkAndMark(key, 60)).isFalse();
            assertThat(idempotent.checkAndMark(otherKey, 60)).isFalse();
            assertThat(idempotent.checkAndMark(key, 60)).isTrue();
            assertThat(idempotent.checkAndMark(otherKey, 60)).isTrue();
            assertThat(idempotent.isDuplicate(key, 60)).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void markExpiresAfterWindow(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            IIdempotent idempotent = new KvStoreIdempotent(fixture.store());
            String key = fixture.key("request:ttl");

            assertThat(idempotent.checkAndMark(key, 1)).isFalse();
            assertThat(idempotent.checkAndMark(key, 1)).isTrue();
            Thread.sleep(1200);

            assertThat(idempotent.isDuplicate(key, 60)).isFalse();
            assertThat(idempotent.checkAndMark(key, 60)).isFalse();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void invalidArguments_throw(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            IIdempotent idempotent = new KvStoreIdempotent(fixture.store());
            String key = fixture.key("request:invalid");

            assertThatThrownBy(() -> idempotent.checkAndMark(null, 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> idempotent.checkAndMark("  ", 60)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> idempotent.checkAndMark(key, 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
