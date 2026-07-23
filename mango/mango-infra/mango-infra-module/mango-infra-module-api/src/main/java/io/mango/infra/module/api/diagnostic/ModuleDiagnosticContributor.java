package io.mango.infra.module.api.diagnostic;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Collection;

/**
 * Read-only module diagnostic extension point owned by one capability domain.
 */
@LocalCapabilityContract
public interface ModuleDiagnosticContributor {

    /**
     * Observes conditions for the explicit immutable request scope.
     * Implementations must not trigger repair, migration or synchronization.
     *
     * @param request explicit diagnostic scope
     * @return bounded safe conditions
     */
    Collection<ModuleDiagnosticCondition> contribute(ModuleDiagnosticRequest request);
}
