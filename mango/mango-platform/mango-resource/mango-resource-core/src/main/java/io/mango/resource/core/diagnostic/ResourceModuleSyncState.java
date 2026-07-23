package io.mango.resource.core.diagnostic;

/** Current process observation state for one Resource declaration module. */
public enum ResourceModuleSyncState {
    /** A synchronization using the current declaration fingerprint is running. */
    RUNNING,
    /** The current fingerprint completed and registry rows match. */
    APPLIED,
    /** Synchronization or registry verification failed. */
    FAILED,
    /** The current fingerprint cannot be proven. */
    UNKNOWN
}
