package io.mango.infra.module.core.diagnostic;

import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticContributor;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleRuntimeStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleDiagnosticAggregatorTest {

    @Test
    void missingRequiredContributorNeverBecomesReady() {
        ModuleDiagnosticContributor installation = request -> List.of(condition(
                ModuleDiagnosticProfile.INSTALLATION, ModuleConditionStatus.PASS, "MODULE_INSTALLED"));
        ModuleDiagnosticAggregator aggregator = new ModuleDiagnosticAggregator(List.of(installation));

        var report = aggregator.diagnose(request(ModuleDiagnosticProfile.ADMIN_MODULE_RUNTIME_V1));

        assertEquals(ModuleRuntimeStatus.UNKNOWN, report.status());
        assertTrue(report.conditions().stream().anyMatch(condition ->
                ModuleDiagnosticProfile.PERSISTENCE_FLYWAY.equals(condition.id())
                        && "MISSING_CONTRIBUTOR".equals(condition.reasonCode())));
    }

    @Test
    void requiredFailureWinsOverUnknown() {
        ModuleDiagnosticContributor contributor = request -> List.of(
                condition(ModuleDiagnosticProfile.INSTALLATION, ModuleConditionStatus.PASS, "MODULE_INSTALLED"),
                condition(ModuleDiagnosticProfile.PERSISTENCE_FLYWAY, ModuleConditionStatus.FAIL, "MIGRATION_FAILED"));
        ModuleDiagnosticAggregator aggregator = new ModuleDiagnosticAggregator(List.of(contributor));

        var report = aggregator.diagnose(request(new ModuleDiagnosticProfile(
                "TEST", SetSupport.of(
                        ModuleDiagnosticProfile.INSTALLATION,
                        ModuleDiagnosticProfile.PERSISTENCE_FLYWAY))));

        assertEquals(ModuleRuntimeStatus.FAILED, report.status());
    }

    @Test
    void optionalUnknownOnlyMarksIncomplete() {
        ModuleDiagnosticContributor contributor = request -> List.of(
                condition(ModuleDiagnosticProfile.INSTALLATION, ModuleConditionStatus.PASS, "MODULE_INSTALLED"),
                condition("optional.version", ModuleConditionStatus.UNKNOWN, "VERSION_UNKNOWN"));
        ModuleDiagnosticAggregator aggregator = new ModuleDiagnosticAggregator(List.of(contributor));

        var report = aggregator.diagnose(request(ModuleDiagnosticProfile.INSTALLATION_ONLY));

        assertEquals(ModuleRuntimeStatus.READY, report.status());
        assertTrue(report.incompleteOptional());
    }

    @Test
    void requiredWarningIsDegradedInsteadOfReady() {
        ModuleDiagnosticContributor contributor = request -> List.of(condition(
                ModuleDiagnosticProfile.INSTALLATION, ModuleConditionStatus.WARN, "VERSION_UNVERIFIED"));
        ModuleDiagnosticAggregator aggregator = new ModuleDiagnosticAggregator(List.of(contributor));

        var report = aggregator.diagnose(request(ModuleDiagnosticProfile.INSTALLATION_ONLY));

        assertEquals(ModuleRuntimeStatus.DEGRADED, report.status());
    }

    private ModuleDiagnosticRequest request(ModuleDiagnosticProfile profile) {
        return new ModuleDiagnosticRequest("mango-link", "internal-admin", profile, Map.of());
    }

    private ModuleDiagnosticCondition condition(String id, ModuleConditionStatus status, String reason) {
        return new ModuleDiagnosticCondition(id, status, false, reason, Map.of(), Instant.EPOCH, 1, false);
    }

    private static final class SetSupport {
        private SetSupport() {
        }

        private static java.util.Set<String> of(String first, String second) {
            return java.util.Set.of(first, second);
        }
    }
}
