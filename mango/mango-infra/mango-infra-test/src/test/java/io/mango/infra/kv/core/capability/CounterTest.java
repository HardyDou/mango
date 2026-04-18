package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.ICounter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CounterTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void increment_supportsPositiveAndNegativeDelta(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICounter counter = new KvStoreCounter(fixture.store());

            String key = fixture.key("sms:1");
            assertThat(counter.increment(key, 5, 60)).isEqualTo(5);
            assertThat(counter.increment(key, -2, 60)).isEqualTo(3);
            assertThat(counter.get(key)).isEqualTo(3);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void keys_areIsolated(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICounter counter = new KvStoreCounter(fixture.store());
            String key1 = fixture.key("sms:1");
            String key2 = fixture.key("sms:2");

            assertThat(counter.increment(key1, 5, 60)).isEqualTo(5);
            assertThat(counter.increment(key2, 2, 60)).isEqualTo(2);

            assertThat(counter.get(key1)).isEqualTo(5);
            assertThat(counter.get(key2)).isEqualTo(2);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void expiredCounterRestartsFromDelta(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICounter counter = new KvStoreCounter(fixture.store());
            String key = fixture.key("sms:ttl");

            assertThat(counter.increment(key, 5, 1)).isEqualTo(5);
            Thread.sleep(1200);

            assertThat(counter.get(key)).isEqualTo(0);
            assertThat(counter.increment(key, 2, 60)).isEqualTo(2);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void invalidArguments_throw(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICounter counter = new KvStoreCounter(fixture.store());
            String key = fixture.key("sms:invalid");

            assertThatThrownBy(() -> counter.increment(null, 1, 60))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> counter.increment("  ", 1, 60))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> counter.increment(key, 1, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
