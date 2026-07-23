package io.mango.infra.module.api.diagnostic;

/**
 * Conservative aggregate status for one observed module.
 */
public enum ModuleRuntimeStatus {
    /** Every applicable required condition is satisfied. */
    READY,
    /** Required conditions pass, while an optional condition reports a risk. */
    DEGRADED,
    /** At least one required condition is known to be unsatisfied. */
    FAILED,
    /** Required evidence is missing, stale or unavailable. */
    UNKNOWN,
    /** The module is explicitly disabled. */
    DISABLED
}
