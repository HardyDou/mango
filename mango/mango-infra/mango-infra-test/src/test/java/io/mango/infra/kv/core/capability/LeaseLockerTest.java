package io.mango.infra.kv.core.capability;

import io.mango.common.exception.BizException;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.api.LockLease;
import io.mango.infra.kv.core.KvStoreTestFixtures.StoreFixture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.mango.infra.kv.core.KvStoreTestFixtures.kvStores;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaseLockerTest {

    static Stream<org.junit.jupiter.params.provider.Arguments> leaseStores() {
        return kvStores();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("leaseStores")
    void onlyCurrentTokenCanReleaseLease(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ILeaseLocker locker = new KvStoreLeaseLocker(fixture.store());
            String key = fixture.key("lease:owner-release");
            LockLease lease = locker.tryAcquire(key, "node-a", 60).orElseThrow();
            LockLease wrongToken = new LockLease(
                    lease.key(), lease.owner(), lease.token() + "-stale", lease.acquiredAt(), lease.leaseUntil());

            assertThat(locker.release(wrongToken)).isFalse();
            assertThat(locker.tryAcquire(key, "node-b", 60)).isEmpty();
            assertThat(locker.release(lease)).isTrue();
            assertThat(locker.tryAcquire(key, "node-b", 60)).isPresent();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("leaseStores")
    void expiredOwnerCannotReleaseOrRenewNewOwnerLease(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ILeaseLocker locker = new KvStoreLeaseLocker(fixture.store());
            String key = fixture.key("lease:late-release");
            LockLease oldLease = locker.tryAcquire(key, "node-a", 1).orElseThrow();
            Thread.sleep(1200);
            LockLease newLease = locker.tryAcquire(key, "node-b", 60).orElseThrow();

            assertThat(locker.renew(oldLease, 60)).isEmpty();
            assertThat(locker.release(oldLease)).isFalse();
            assertThat(locker.renew(newLease, 60)).isPresent();
            assertThat(locker.tryAcquire(key, "node-c", 60)).isEmpty();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("leaseStores")
    void sameOwnerGetsUniqueTokenForEachAcquisition(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ILeaseLocker locker = new KvStoreLeaseLocker(fixture.store());
            String key = fixture.key("lease:same-owner");
            LockLease oldLease = locker.tryAcquire(key, "node-a", 1).orElseThrow();
            Thread.sleep(1200);
            LockLease newLease = locker.tryAcquire(key, "node-a", 60).orElseThrow();

            assertThat(newLease.token()).isNotEqualTo(oldLease.token());
            assertThat(locker.release(oldLease)).isFalse();
            assertThat(locker.renew(newLease, 60)).isPresent();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("leaseStores")
    void invalidArgumentsFailFast(String name, StoreFixture fixture) throws Exception {
        try (fixture) {
            ILeaseLocker locker = new KvStoreLeaseLocker(fixture.store());

            assertThatThrownBy(() -> locker.tryAcquire(" ", "node-a", 60))
                    .isInstanceOf(BizException.class);
            assertThatThrownBy(() -> locker.tryAcquire(fixture.key("lease:key"), " ", 60))
                    .isInstanceOf(BizException.class);
            assertThatThrownBy(() -> locker.tryAcquire(fixture.key("lease:key"), "node-a", 0))
                    .isInstanceOf(BizException.class);
            assertThatThrownBy(() -> locker.renew(null, 60))
                    .isInstanceOf(BizException.class);
            assertThatThrownBy(() -> locker.release(null))
                    .isInstanceOf(BizException.class);
        }
    }
}
