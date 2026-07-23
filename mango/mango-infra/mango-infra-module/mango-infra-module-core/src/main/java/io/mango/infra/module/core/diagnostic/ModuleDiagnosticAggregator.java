package io.mango.infra.module.core.diagnostic;

import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticContributor;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticReport;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleRuntimeStatus;
import io.mango.infra.module.api.diagnostic.ModuleVersionEvidence;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conservative, deterministic aggregation of independent diagnostic contributors.
 */
public class ModuleDiagnosticAggregator {

    private static final int SEVERITY_SKIPPED = 1;
    private static final int SEVERITY_PASS = 2;
    private static final int SEVERITY_WARN = 3;
    private static final int SEVERITY_UNKNOWN = 4;
    private static final int SEVERITY_FAIL = 5;

    private final List<ModuleDiagnosticContributor> contributors;
    private final Clock clock;

    public ModuleDiagnosticAggregator(Collection<ModuleDiagnosticContributor> contributors) {
        this(contributors, Clock.systemUTC());
    }

    ModuleDiagnosticAggregator(Collection<ModuleDiagnosticContributor> contributors, Clock clock) {
        this.contributors = contributors == null ? List.of() : List.copyOf(contributors);
        this.clock = clock;
    }

    /**
     * Aggregates a single module without allowing a contributor failure to hide other evidence.
     */
    public ModuleDiagnosticReport diagnose(ModuleDiagnosticRequest request) {
        Instant observedAt = clock.instant();
        Map<String, ModuleDiagnosticCondition> conditions = new LinkedHashMap<>();
        for (ModuleDiagnosticContributor contributor : contributors) {
            Collection<ModuleDiagnosticCondition> contributed = contributeSafely(contributor, request, observedAt);
            if (contributed == null) {
                continue;
            }
            for (ModuleDiagnosticCondition condition : contributed) {
                if (condition == null) {
                    continue;
                }
                conditions.merge(condition.id(), condition, this::preferMoreSevere);
            }
        }
        ModuleDiagnosticProfile profile = request.profile();
        profile.requiredConditionIds().forEach(conditionId -> conditions.computeIfAbsent(
                conditionId,
                ignored -> ModuleDiagnosticCondition.missingContributor(conditionId, observedAt)));

        List<ModuleDiagnosticCondition> normalized = conditions.values().stream()
                .map(condition -> normalizeRequired(condition, profile))
                .sorted(Comparator.comparing(ModuleDiagnosticCondition::id))
                .toList();
        ModuleRuntimeStatus status = aggregateStatus(normalized);
        boolean incompleteOptional = normalized.stream()
                .anyMatch(condition -> !condition.required()
                        && condition.status() == ModuleConditionStatus.UNKNOWN);
        ModuleVersionEvidence unknownExpected = new ModuleVersionEvidence(
                null, "NONE", ModuleConditionStatus.UNKNOWN, "NO_EXPECTATION_PROVIDER");
        ModuleVersionEvidence backendVersion = findVersion(normalized, "backendVersion");
        return new ModuleDiagnosticReport(
                request.moduleCode(),
                status,
                incompleteOptional,
                backendVersion,
                new ModuleVersionEvidence(null, "BROWSER_REPORT", ModuleConditionStatus.UNKNOWN,
                        "FRONTEND_REPORT_PENDING"),
                unknownExpected,
                normalized);
    }

    private Collection<ModuleDiagnosticCondition> contributeSafely(
            ModuleDiagnosticContributor contributor,
            ModuleDiagnosticRequest request,
            Instant observedAt) {
        try {
            return contributor.contribute(request);
        } catch (RuntimeException exception) {
            String contributorName = contributor.getClass().getName();
            return List.of(new ModuleDiagnosticCondition(
                    "contributor." + Integer.toHexString(contributorName.hashCode()),
                    ModuleConditionStatus.UNKNOWN,
                    false,
                    "CONTRIBUTOR_ERROR",
                    Map.of("contributor", contributor.getClass().getSimpleName()),
                    observedAt,
                    0,
                    false));
        }
    }

    private ModuleDiagnosticCondition normalizeRequired(
            ModuleDiagnosticCondition condition,
            ModuleDiagnosticProfile profile) {
        boolean required = profile.requiredConditionIds().contains(condition.id());
        return new ModuleDiagnosticCondition(
                condition.id(),
                condition.status(),
                required,
                condition.reasonCode(),
                condition.evidence(),
                condition.observedAt(),
                condition.durationMs(),
                condition.stale());
    }

    private ModuleDiagnosticCondition preferMoreSevere(
            ModuleDiagnosticCondition left,
            ModuleDiagnosticCondition right) {
        return severity(right) > severity(left) ? right : left;
    }

    private int severity(ModuleDiagnosticCondition condition) {
        if (condition.stale()) {
            return SEVERITY_UNKNOWN;
        }
        return switch (condition.status()) {
            case FAIL -> SEVERITY_FAIL;
            case UNKNOWN -> SEVERITY_UNKNOWN;
            case WARN -> SEVERITY_WARN;
            case PASS -> SEVERITY_PASS;
            case SKIPPED -> SEVERITY_SKIPPED;
        };
    }

    private ModuleRuntimeStatus aggregateStatus(List<ModuleDiagnosticCondition> conditions) {
        if (conditions.stream().anyMatch(condition -> condition.required()
                && condition.status() == ModuleConditionStatus.FAIL)) {
            return ModuleRuntimeStatus.FAILED;
        }
        if (conditions.stream().anyMatch(condition -> condition.required()
                && (condition.status() == ModuleConditionStatus.UNKNOWN || condition.stale()))) {
            return ModuleRuntimeStatus.UNKNOWN;
        }
        if (conditions.stream().anyMatch(condition -> condition.required()
                && condition.status() == ModuleConditionStatus.WARN)) {
            return ModuleRuntimeStatus.DEGRADED;
        }
        if (conditions.stream().anyMatch(condition -> !condition.required()
                && (condition.status() == ModuleConditionStatus.FAIL
                || condition.status() == ModuleConditionStatus.WARN))) {
            return ModuleRuntimeStatus.DEGRADED;
        }
        return ModuleRuntimeStatus.READY;
    }

    private ModuleVersionEvidence findVersion(List<ModuleDiagnosticCondition> conditions, String evidenceKey) {
        for (ModuleDiagnosticCondition condition : conditions) {
            Object value = condition.evidence().get(evidenceKey);
            if (value instanceof String version && !version.isBlank()) {
                Object source = condition.evidence().get("versionSource");
                return new ModuleVersionEvidence(
                        version,
                        source instanceof String text ? text : "UNKNOWN",
                        ModuleConditionStatus.PASS,
                        "VERSION_OBSERVED");
            }
        }
        return new ModuleVersionEvidence(null, "NONE", ModuleConditionStatus.UNKNOWN, "VERSION_UNKNOWN");
    }
}
