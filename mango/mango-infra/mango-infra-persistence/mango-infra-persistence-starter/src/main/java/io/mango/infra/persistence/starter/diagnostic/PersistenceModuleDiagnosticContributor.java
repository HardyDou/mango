package io.mango.infra.persistence.starter.diagnostic;

import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticContributor;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.infra.persistence.starter.PersistenceFlywayProperties;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps retained Flyway domain state to the neutral module diagnostic contract.
 */
public class PersistenceModuleDiagnosticContributor implements ModuleDiagnosticContributor {

    private final PersistenceModuleMigrationStatusRegistry registry;
    private final boolean enabled;
    private final Set<String> disabledModules;

    public PersistenceModuleDiagnosticContributor(
            PersistenceModuleMigrationStatusRegistry registry,
            PersistenceFlywayProperties properties) {
        this.registry = registry;
        this.enabled = properties.isEnabled();
        this.disabledModules = properties.getModules().entrySet().stream()
                .filter(entry -> !entry.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<ModuleDiagnosticCondition> contribute(ModuleDiagnosticRequest request) {
        Instant observedAt = Instant.now();
        String persistenceModule = request.attributes().get(ModuleInstallation.PERSISTENCE_MODULE_ATTRIBUTE);
        if (persistenceModule == null || persistenceModule.isBlank()) {
            return List.of(condition(
                    ModuleConditionStatus.UNKNOWN,
                    "MAPPING_UNRESOLVED",
                    Map.of(),
                    observedAt,
                    0));
        }
        if (!enabled) {
            return List.of(condition(
                    ModuleConditionStatus.SKIPPED,
                    "FLYWAY_GLOBALLY_DISABLED",
                    Map.of("persistenceModule", persistenceModule),
                    observedAt,
                    0));
        }
        if (disabledModules.contains(persistenceModule)) {
            return List.of(condition(
                    ModuleConditionStatus.SKIPPED,
                    "FLYWAY_MODULE_DISABLED",
                    Map.of("persistenceModule", persistenceModule),
                    observedAt,
                    0));
        }
        return registry.resolve(persistenceModule)
                .map(status -> List.of(toCondition(status)))
                .orElseGet(() -> List.of(condition(
                        ModuleConditionStatus.UNKNOWN,
                        "MIGRATION_NOT_OBSERVED",
                        Map.of("persistenceModule", persistenceModule),
                        observedAt,
                        0)));
    }

    private ModuleDiagnosticCondition toCondition(PersistenceModuleMigrationStatus status) {
        ModuleConditionStatus conditionStatus = switch (status.state()) {
            case APPLIED -> ModuleConditionStatus.PASS;
            case FAILED -> ModuleConditionStatus.FAIL;
            case DISABLED -> ModuleConditionStatus.SKIPPED;
            case RUNNING, UNKNOWN -> ModuleConditionStatus.UNKNOWN;
        };
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("persistenceModule", status.module());
        evidence.put("state", status.state().name());
        evidence.put("historyTable", status.historyTable());
        evidence.put("pendingCount", status.pendingCount());
        if (status.currentVersion() != null) {
            evidence.put("currentVersion", status.currentVersion());
        }
        return condition(
                conditionStatus,
                status.reasonCode(),
                evidence,
                status.observedAt(),
                status.durationMs());
    }

    private ModuleDiagnosticCondition condition(
            ModuleConditionStatus status,
            String reasonCode,
            Map<String, Object> evidence,
            Instant observedAt,
            long durationMs) {
        return new ModuleDiagnosticCondition(
                ModuleDiagnosticProfile.PERSISTENCE_FLYWAY,
                status,
                true,
                reasonCode,
                evidence,
                observedAt,
                durationMs,
                false);
    }
}
