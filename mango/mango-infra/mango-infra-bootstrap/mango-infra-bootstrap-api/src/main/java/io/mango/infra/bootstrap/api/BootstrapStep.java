package io.mango.infra.bootstrap.api;

import java.util.Set;

public interface BootstrapStep {

    String code();

    BootstrapPhase phase();

    default Set<String> dependencies() {
        return Set.of();
    }

    /** Dependencies that affect ordering when the referenced capability is installed. */
    default Set<String> optionalDependencies() {
        return Set.of();
    }

    String fingerprintMaterial();

    BootstrapStepResult execute(BootstrapExecutionContext context);
}
