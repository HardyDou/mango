package io.mango.infra.persistence.starter.diagnostic;

/**
 * Domain state retained by the existing per-module Flyway execution flow.
 */
public enum PersistenceMigrationState {
    /** Migration execution is in progress. */
    RUNNING,
    /** Migration execution completed and no pending migration remains. */
    APPLIED,
    /** Migration execution failed. */
    FAILED,
    /** Migration is explicitly disabled. */
    DISABLED,
    /** No authoritative observation is available. */
    UNKNOWN
}
