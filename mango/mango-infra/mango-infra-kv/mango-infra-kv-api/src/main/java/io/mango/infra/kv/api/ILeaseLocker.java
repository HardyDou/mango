package io.mango.infra.kv.api;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Optional;

/**
 * Owner-safe distributed lease capability.
 *
 * <p>Each successful acquisition returns a unique token. Renewal and release
 * only succeed while that token still owns the live lease.</p>
 */
@LocalCapabilityContract
public interface ILeaseLocker {

    /**
     * Attempts to acquire a lease.
     *
     * @param key business lock key
     * @param owner observable instance or batch owner
     * @param ttlSeconds positive lease TTL in seconds
     * @return owned lease handle, or empty when another live lease exists
     */
    Optional<LockLease> tryAcquire(String key, String owner, long ttlSeconds);

    /**
     * Renews a lease only while its unique token remains the current owner.
     *
     * @param lease acquired lease handle
     * @param ttlSeconds positive renewed TTL in seconds
     * @return renewed lease observation, or empty when ownership was lost
     */
    Optional<LockLease> renew(LockLease lease, long ttlSeconds);

    /**
     * Releases a lease only while its unique token remains the current owner.
     *
     * @param lease acquired lease handle
     * @return true when the matching live lease was removed
     */
    boolean release(LockLease lease);
}
