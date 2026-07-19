package io.mango.resource.support.sync;

/**
 * Exposes whether the current application's startup resource synchronization has completed.
 */
@FunctionalInterface
public interface ResourceSynchronizationStatus {

    /**
     * Returns whether resource declarations are ready for dependent reconciliation.
     *
     * @return {@code true} after synchronization succeeds or is intentionally skipped
     */
    boolean isSynchronizationComplete();
}
