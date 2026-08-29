package io.mango.resource.api.enums;

/**
 * Resource target synchronization outcome.
 */
public enum ResourceSyncDisposition {

    /** The incoming declaration was written to the target. */
    APPLIED,

    /** No target write was needed. */
    SKIPPED,

    /** Runtime-managed target data was retained instead of being overwritten. */
    PRESERVED,

    /** The target operation failed. */
    FAILED
}
