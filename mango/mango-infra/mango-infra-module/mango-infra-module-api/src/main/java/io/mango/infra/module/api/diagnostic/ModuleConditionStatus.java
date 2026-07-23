package io.mango.infra.module.api.diagnostic;

/**
 * A single observed module condition.
 */
public enum ModuleConditionStatus {
    /** Current authoritative evidence proves the condition. */
    PASS,
    /** The condition is usable but carries a non-blocking risk. */
    WARN,
    /** Current evidence proves the condition is not satisfied. */
    FAIL,
    /** The condition cannot be observed or proven. */
    UNKNOWN,
    /** Authoritative evidence proves the condition is not applicable. */
    SKIPPED
}
