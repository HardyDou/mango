package io.mango.infra.bootstrap.api;

import java.util.List;

@FunctionalInterface
public interface BootstrapStepContributor {

    List<BootstrapStep> contributeSteps();

    /**
     * Whether this contributor may run while a portable Resource database baseline is built.
     *
     * <p>The default is fail-closed because application contributors may depend on deployment
     * credentials, remote systems, or runtime-only state.</p>
     */
    default boolean supportsResourceBaselineBuild() {
        return false;
    }
}
