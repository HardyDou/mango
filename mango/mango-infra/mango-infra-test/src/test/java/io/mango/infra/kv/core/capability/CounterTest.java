package io.mango.infra.kv.core.capability;

import io.mango.common.exception.BizException;
import io.mango.infra.kv.api.ICounter;
import io.mango.infra.kv.core.KvStoreTestFixtures.StoreFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CounterTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void increment_supportsDeltaAndIsolation(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ICounter counter = new KvStoreCounter(fixture.store());
            String key = fixture.key("sms:1");
            String otherKey = fixture.key("sms:2");

            assertThat(counter.increment(key, 5, 60)).isEqualTo(5);
            assertThat(counter.increment(key, -2, 60)).isEqualTo(3);
            assertThat(counter.increment(otherKey, 2, 60)).isEqualTo(2);

            assertThat(counter.get(key)).isEqualTo(3);
            assertThat(counter.get(otherKey)).isEqualTo(2);
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

            assertThatThrownBy(() -> counter.increment(null, 1, 60)).isInstanceOf(BizException.class);
            assertThatThrownBy(() -> counter.increment("  ", 1, 60)).isInstanceOf(BizException.class);
            assertThatThrownBy(() -> counter.increment(key, 1, 0)).isInstanceOf(BizException.class);
        }
    }
}
