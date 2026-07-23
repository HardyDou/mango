package io.mango.infra.persistence.starter.diagnostic;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe read-only observation surface for the existing Flyway initializer.
 */
public class PersistenceModuleMigrationStatusRegistry {

    private final Map<String, PersistenceModuleMigrationStatus> statuses = new ConcurrentHashMap<>();
    private final Map<String, Instant> starts = new ConcurrentHashMap<>();
    private final Clock clock;

    public PersistenceModuleMigrationStatusRegistry() {
        this(Clock.systemUTC());
    }

    PersistenceModuleMigrationStatusRegistry(Clock clock) {
        this.clock = clock;
    }

    public void running(String module, String historyTable) {
        Instant now = clock.instant();
        starts.put(module, now);
        statuses.put(module, new PersistenceModuleMigrationStatus(
                module, PersistenceMigrationState.RUNNING, historyTable, null, 0,
                now, 0, "MIGRATION_RUNNING"));
    }

    public void applied(String module, String historyTable, String currentVersion, int pendingCount) {
        Instant now = clock.instant();
        statuses.put(module, new PersistenceModuleMigrationStatus(
                module,
                pendingCount == 0 ? PersistenceMigrationState.APPLIED : PersistenceMigrationState.UNKNOWN,
                historyTable,
                currentVersion,
                pendingCount,
                now,
                duration(module, now),
                pendingCount == 0 ? "MIGRATION_APPLIED" : "MIGRATION_PENDING"));
        starts.remove(module);
    }

    public void failed(String module, String historyTable) {
        Instant now = clock.instant();
        statuses.put(module, new PersistenceModuleMigrationStatus(
                module, PersistenceMigrationState.FAILED, historyTable, null, 0,
                now, duration(module, now), "MIGRATION_FAILED"));
        starts.remove(module);
    }

    public void unknown(String module, String historyTable, String reasonCode) {
        Instant now = clock.instant();
        statuses.put(module, new PersistenceModuleMigrationStatus(
                module, PersistenceMigrationState.UNKNOWN, historyTable, null, 0,
                now, duration(module, now), reasonCode));
        starts.remove(module);
    }

    public Optional<PersistenceModuleMigrationStatus> resolve(String module) {
        if (module == null || module.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(statuses.get(module.trim()));
    }

    public Collection<PersistenceModuleMigrationStatus> list() {
        return statuses.values().stream()
                .sorted(Comparator.comparing(PersistenceModuleMigrationStatus::module))
                .toList();
    }

    private long duration(String module, Instant now) {
        Instant start = starts.get(module);
        return start == null ? 0 : Math.max(0, java.time.Duration.between(start, now).toMillis());
    }
}
