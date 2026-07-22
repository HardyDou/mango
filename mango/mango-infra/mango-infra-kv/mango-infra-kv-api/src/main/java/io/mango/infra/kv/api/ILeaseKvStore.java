package io.mango.infra.kv.api;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * Storage-side atomic operations required by {@link ILeaseLocker}.
 *
 * <p>Custom KV stores used for leases must implement all three operations as
 * single atomic storage operations. Implementations must not emulate them with
 * a client-side read followed by a write or delete.</p>
 */
@LocalCapabilityContract
public interface ILeaseKvStore extends IKvStore {

    /**
     * Stores a token only when no live lease exists.
     */
    boolean tryAcquireLease(String key, String token, long ttlSeconds);

    /**
     * Extends expiry only when the current live value equals the token.
     */
    boolean renewLease(String key, String token, long ttlSeconds);

    /**
     * Deletes only when the current live value equals the token.
     */
    boolean releaseLease(String key, String token);
}
