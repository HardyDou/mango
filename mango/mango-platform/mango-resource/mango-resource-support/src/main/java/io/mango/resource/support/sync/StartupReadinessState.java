package io.mango.resource.support.sync;

/**
 * Startup state exposed by resource synchronization and its dependent reconciliation steps.
 */
public enum StartupReadinessState {

    /** Startup work has not begun. */
    BOOTSTRAPPING,

    /** Resource declarations are being synchronized. */
    SYNCING,

    /** A transient failure is waiting for its next retry window. */
    TRANSIENT_WAIT,

    /** The current immutable declaration snapshot has a deterministic failure. */
    PERMANENT_FAILED,

    /** Tenant baselines are being reconciled after resource synchronization. */
    RECONCILING_TENANTS,

    /** All work owned by the participant has converged. */
    READY,

    /** The application is shutting down and no new work is accepted. */
    SHUTTING_DOWN
}
