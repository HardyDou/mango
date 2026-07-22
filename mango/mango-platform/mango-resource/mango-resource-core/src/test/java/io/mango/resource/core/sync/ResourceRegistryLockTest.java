package io.mango.resource.core.sync;

import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.api.LockLease;
import io.mango.infra.kv.core.capability.KvStoreLeaseLocker;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceRegistryLockTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:resource_lock_" + System.nanoTime()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        jdbcTemplate = new JdbcTemplate(dataSource);
        createKvTable();
    }

    @Test
    void lockUsesJdbcKvStoreSemantics() {
        ResourceRegistryLock registryLock = new ResourceRegistryLock(
                new KvStoreLeaseLocker(new JdbcKvStore(jdbcTemplate)));

        ResourceRegistryLock.LeaseSession nodeA = registryLock.tryLock("node-a", 60).orElseThrow();
        assertThat(registryLock.tryLock("node-b", 60)).isEmpty();
        assertThat(countLockRows()).isEqualTo(1);

        nodeA.close();

        assertThat(countLockRows()).isZero();
        assertThat(registryLock.tryLock("node-b", 60)).isPresent();
        registryLock.close();
    }

    @Test
    void renewsActiveLeaseAndKeepsOwnership() throws Exception {
        ControllableLeaseLocker locker = new ControllableLeaseLocker();
        ResourceRegistryLock registryLock = new ResourceRegistryLock(locker);
        ResourceRegistryLock.LeaseSession session = registryLock.tryLock("node-a", 1).orElseThrow();

        assertThat(locker.awaitRenewal(5, TimeUnit.SECONDS)).isTrue();
        assertThat(locker.renewCount()).isGreaterThanOrEqualTo(1);
        assertThat(session.isOwned()).isTrue();
        registryLock.assertOwned();

        registryLock.close();
        assertThat(locker.releaseCount()).isEqualTo(1);
    }

    @Test
    void failedRenewalLosesOwnershipAndStaleCloseDoesNotReleaseSuccessor() throws Exception {
        ControllableLeaseLocker locker = new ControllableLeaseLocker();
        locker.failRenewals();
        ResourceRegistryLock registryLock = new ResourceRegistryLock(locker);
        ResourceRegistryLock.LeaseSession staleSession = registryLock.tryLock("node-a", 1).orElseThrow();

        assertThat(locker.awaitRenewal(5, TimeUnit.SECONDS)).isTrue();
        assertThat(staleSession.isOwned()).isFalse();
        assertThatThrownBy(registryLock::assertOwned)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Resource Registry lease ownership was lost");

        staleSession.close();
        locker.allowRenewals();
        ResourceRegistryLock.LeaseSession successor = registryLock.tryLock("node-b", 60).orElseThrow();
        staleSession.close();

        assertThat(locker.currentOwner()).contains("node-b");
        assertThat(locker.releaseCount()).isZero();
        successor.close();
        assertThat(locker.releaseCount()).isEqualTo(1);
        registryLock.close();
    }

    @Test
    void closeReleasesActiveLeaseAndRejectsNewAcquisition() {
        ControllableLeaseLocker locker = new ControllableLeaseLocker();
        ResourceRegistryLock registryLock = new ResourceRegistryLock(locker);
        registryLock.tryLock("node-a", 60).orElseThrow();

        registryLock.close();

        assertThat(locker.releaseCount()).isEqualTo(1);
        assertThat(locker.currentOwner()).isEmpty();
        assertThatThrownBy(() -> registryLock.tryLock("node-b", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Resource Registry lock is closed");
        assertThat(locker.acquireCount()).isEqualTo(1);
    }

    private void createKvTable() {
        jdbcTemplate.execute("""
                CREATE TABLE infra_kv_entry (
                    id          BIGINT NOT NULL,
                    kv_key      VARCHAR(200) NOT NULL,
                    kv_value    TEXT,
                    expire_time DATETIME NOT NULL,
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_kv_key (kv_key)
                )
                """);
    }

    private long countLockRows() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM infra_kv_entry
                WHERE kv_key = ?
                """, Long.class, ResourceRegistryLock.LOCK_NAME);
    }

    private static final class ControllableLeaseLocker implements ILeaseLocker {

        private final AtomicReference<LockLease> currentLease = new AtomicReference<>();
        private final AtomicInteger acquireCount = new AtomicInteger();
        private final AtomicInteger renewCount = new AtomicInteger();
        private final AtomicInteger releaseCount = new AtomicInteger();
        private final CountDownLatch renewalAttempted = new CountDownLatch(1);
        private volatile boolean renewalsAllowed = true;

        @Override
        public Optional<LockLease> tryAcquire(String key, String owner, long ttlSeconds) {
            acquireCount.incrementAndGet();
            Instant now = Instant.now();
            LockLease lease = new LockLease(
                    key, owner, owner + ":" + UUID.randomUUID(), now, now.plusSeconds(ttlSeconds));
            return currentLease.compareAndSet(null, lease) ? Optional.of(lease) : Optional.empty();
        }

        @Override
        public Optional<LockLease> renew(LockLease lease, long ttlSeconds) {
            renewCount.incrementAndGet();
            renewalAttempted.countDown();
            LockLease current = currentLease.get();
            if (!renewalsAllowed || current == null || !current.token().equals(lease.token())) {
                currentLease.compareAndSet(current, null);
                return Optional.empty();
            }
            LockLease renewed = lease.renewedUntil(Instant.now().plusSeconds(ttlSeconds));
            return currentLease.compareAndSet(current, renewed) ? Optional.of(renewed) : Optional.empty();
        }

        @Override
        public boolean release(LockLease lease) {
            LockLease current = currentLease.get();
            if (current == null || !current.token().equals(lease.token())) {
                return false;
            }
            if (!currentLease.compareAndSet(current, null)) {
                return false;
            }
            releaseCount.incrementAndGet();
            return true;
        }

        void failRenewals() {
            renewalsAllowed = false;
        }

        void allowRenewals() {
            renewalsAllowed = true;
        }

        boolean awaitRenewal(long timeout, TimeUnit unit) throws InterruptedException {
            return renewalAttempted.await(timeout, unit);
        }

        int acquireCount() {
            return acquireCount.get();
        }

        int renewCount() {
            return renewCount.get();
        }

        int releaseCount() {
            return releaseCount.get();
        }

        Optional<String> currentOwner() {
            return Optional.ofNullable(currentLease.get()).map(LockLease::owner);
        }
    }
}
