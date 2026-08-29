package io.mango.resource.support;

/** Defines whether a Resource handler may materialize data into a portable database baseline. */
public enum ResourceBaselinePolicy {

    /** The handler only writes portable database state and may run during baseline generation. */
    PORTABLE,

    /** The handler depends on deployment environment state and must run after baseline restore. */
    ENVIRONMENT_REQUIRED
}
