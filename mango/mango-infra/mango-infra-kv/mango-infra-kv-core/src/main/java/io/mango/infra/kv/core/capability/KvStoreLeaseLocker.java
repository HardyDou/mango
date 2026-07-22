package io.mango.infra.kv.core.capability;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.ILeaseKvStore;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.api.LockLease;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Lease capability backed by storage-side atomic token operations.
 */
public class KvStoreLeaseLocker implements ILeaseLocker {

    private final ILeaseKvStore leaseStore;

    public KvStoreLeaseLocker(IKvStore kvStore) {
        Require.notNull(kvStore, "kvStore cannot be null");
        Require.isTrue(kvStore instanceof ILeaseKvStore,
                "KV store must implement atomic ILeaseKvStore operations for distributed leases");
        this.leaseStore = (ILeaseKvStore) kvStore;
    }

    @Override
    public Optional<LockLease> tryAcquire(String key, String owner, long ttlSeconds) {
        validate(key, owner, ttlSeconds);
        String token = owner.trim() + ":" + UUID.randomUUID();
        if (!leaseStore.tryAcquireLease(key.trim(), token, ttlSeconds)) {
            return Optional.empty();
        }
        Instant acquiredAt = Instant.now();
        return Optional.of(new LockLease(
                key.trim(), owner.trim(), token, acquiredAt, acquiredAt.plusSeconds(ttlSeconds)));
    }

    @Override
    public Optional<LockLease> renew(LockLease lease, long ttlSeconds) {
        Require.notNull(lease, "lease cannot be null");
        Require.positive(ttlSeconds, "ttlSeconds must be positive");
        if (!leaseStore.renewLease(lease.key(), lease.token(), ttlSeconds)) {
            return Optional.empty();
        }
        return Optional.of(lease.renewedUntil(Instant.now().plusSeconds(ttlSeconds)));
    }

    @Override
    public boolean release(LockLease lease) {
        Require.notNull(lease, "lease cannot be null");
        return leaseStore.releaseLease(lease.key(), lease.token());
    }

    private void validate(String key, String owner, long ttlSeconds) {
        Require.notBlank(key, "key cannot be null or blank");
        Require.notBlank(owner, "owner cannot be null or blank");
        Require.positive(ttlSeconds, "ttlSeconds must be positive");
    }
}
