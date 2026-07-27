package io.mango.infra.persistence.starter;

import io.mango.infra.bootstrap.api.BootstrapPhase;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Explicit Flyway entry used only by Mango Bootstrap steps.
 */
public final class PersistenceFlywayBootstrapExecutor {

    private final Function<BootstrapPhase, MigrationSummary> migration;
    private final Supplier<MigrationSummary> coldBaseline;

    PersistenceFlywayBootstrapExecutor(Function<BootstrapPhase, MigrationSummary> migration,
                                        Supplier<MigrationSummary> coldBaseline) {
        this.migration = Objects.requireNonNull(migration, "migration");
        this.coldBaseline = Objects.requireNonNull(coldBaseline, "coldBaseline");
    }

    public MigrationSummary applyColdBaseline() {
        return coldBaseline.get();
    }

    public MigrationSummary migrate(BootstrapPhase phase) {
        if (phase != BootstrapPhase.EXPAND && phase != BootstrapPhase.FINALIZE) {
            throw new IllegalArgumentException("Flyway bootstrap phase must be EXPAND or FINALIZE: " + phase);
        }
        return migration.apply(phase);
    }

    public record MigrationSummary(int moduleCount, int migrationCount, String phase) {
    }
}
