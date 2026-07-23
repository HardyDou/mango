package io.mango.infra.persistence.starter.diagnostic;

import java.time.Instant;

/**
 * Safe process-local Flyway status for one persistence module.
 */
public record PersistenceModuleMigrationStatus(
        String module,
        PersistenceMigrationState state,
        String historyTable,
        String currentVersion,
        int pendingCount,
        Instant observedAt,
        long durationMs,
        String reasonCode) {

    public PersistenceModuleMigrationStatus {
        if (module == null || module.isBlank()) {
            throw new IllegalArgumentException("module must not be blank");
        }
        module = module.trim();
        state = state == null ? PersistenceMigrationState.UNKNOWN : state;
        historyTable = historyTable == null || historyTable.isBlank() ? "unknown" : historyTable.trim();
        if (currentVersion != null && currentVersion.isBlank()) {
            currentVersion = null;
        }
        pendingCount = Math.max(0, pendingCount);
        observedAt = observedAt == null ? Instant.now() : observedAt;
        durationMs = Math.max(0, durationMs);
        reasonCode = reasonCode == null || reasonCode.isBlank() ? "MIGRATION_UNKNOWN" : reasonCode.trim();
    }
}
