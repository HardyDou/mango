package io.mango.infra.bootstrap.api;

import java.util.List;

@FunctionalInterface
public interface BootstrapStepContributor {

    List<BootstrapStep> contributeSteps();
}
