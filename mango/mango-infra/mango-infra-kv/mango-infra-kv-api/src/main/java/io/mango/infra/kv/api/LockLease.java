package io.mango.infra.kv.api;

import io.mango.common.contract.LocalCapabilityContract;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable ownership handle returned for one successful lease acquisition.
 *
 * @param key key stored by the lease implementation
 * @param owner observable owner identity, not the ownership secret
 * @param token unique token for this acquisition
 * @param acquiredAt acquisition time observed by the caller
 * @param leaseUntil approximate lease deadline observed by the caller
 */
@LocalCapabilityContract
public record LockLease(String key, String owner, String token, Instant acquiredAt, Instant leaseUntil) {

    public LockLease {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(acquiredAt, "acquiredAt cannot be null");
        Objects.requireNonNull(leaseUntil, "leaseUntil cannot be null");
    }

    /**
     * Returns a new observation of the same owned lease after successful renewal.
     *
     * @param renewedUntil renewed approximate deadline
     * @return renewed lease observation retaining the acquisition token
     */
    public LockLease renewedUntil(Instant renewedUntil) {
        return new LockLease(key, owner, token, acquiredAt, renewedUntil);
    }
}
