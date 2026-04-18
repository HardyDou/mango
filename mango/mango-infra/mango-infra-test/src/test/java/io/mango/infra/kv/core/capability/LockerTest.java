package io.mango.infra.kv.core.capability;

import io.mango.infra.kv.api.ILocker;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockerTest extends KvStoreCapabilityTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void tryLock_usesSetIfAbsentSemantics(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ILocker locker = new KvStoreLocker(fixture.store());

            String key = fixture.key("order:1");
            assertThat(locker.tryLock(key, 60)).isTrue();
            assertThat(locker.tryLock(key, 60)).isFalse();
            locker.unlock(key);
            assertThat(locker.tryLock(key, 60)).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void keys_areIsolated(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ILocker locker = new KvStoreLocker(fixture.store());
            String key1 = fixture.key("order:1");
            String key2 = fixture.key("order:2");

            assertThat(locker.tryLock(key1, 60)).isTrue();
            assertThat(locker.tryLock(key2, 60)).isTrue();
            assertThat(locker.tryLock(key1, 60)).isFalse();
            assertThat(locker.tryLock(key2, 60)).isFalse();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void expiredLockCanBeAcquiredAgain(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ILocker locker = new KvStoreLocker(fixture.store());
            String key = fixture.key("order:ttl");

            assertThat(locker.tryLock(key, 1)).isTrue();
            assertThat(locker.tryLock(key, 1)).isFalse();
            Thread.sleep(1200);

            assertThat(locker.tryLock(key, 60)).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kvStores")
    void invalidArguments_throw(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ILocker locker = new KvStoreLocker(fixture.store());
            String key = fixture.key("order:invalid");

            assertThatThrownBy(() -> locker.tryLock(null, 60))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> locker.tryLock("  ", 60))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> locker.tryLock(key, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
