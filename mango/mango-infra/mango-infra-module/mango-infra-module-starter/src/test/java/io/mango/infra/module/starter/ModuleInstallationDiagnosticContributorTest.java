package io.mango.infra.module.starter;

import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.core.diagnostic.MemoryModuleInstallationRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleInstallationDiagnosticContributorTest {

    private final ModuleInstallationDiagnosticContributor contributor =
            new ModuleInstallationDiagnosticContributor(new MemoryModuleInstallationRegistry());

    @Test
    void missingModuleOnOneInstanceRemainsUnknown() {
        var condition = contributor.contribute(request(Map.of())).iterator().next();

        assertThat(condition.status()).isEqualTo(ModuleConditionStatus.UNKNOWN);
        assertThat(condition.reasonCode()).isEqualTo("MODULE_NOT_OBSERVED_ON_INSTANCE");
    }

    @Test
    void authoritativeOwnerCanProveThatRequestedModuleIsNotInstalled() {
        var condition = contributor.contribute(request(Map.of(
                ModuleDiagnosticRequest.REPORT_SCOPE_ATTRIBUTE,
                ModuleDiagnosticRequest.AUTHORITATIVE_OWNER_SCOPE))).iterator().next();

        assertThat(condition.status()).isEqualTo(ModuleConditionStatus.FAIL);
        assertThat(condition.reasonCode()).isEqualTo("MODULE_NOT_INSTALLED");
    }

    private ModuleDiagnosticRequest request(Map<String, String> attributes) {
        return new ModuleDiagnosticRequest(
                "mango-link",
                "internal-admin",
                ModuleDiagnosticProfile.INSTALLATION_ONLY,
                attributes);
    }
}
