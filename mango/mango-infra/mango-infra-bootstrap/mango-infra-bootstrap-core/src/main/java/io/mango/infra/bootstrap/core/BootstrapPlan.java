package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapStep;

import java.util.List;

public record BootstrapPlan(String manifestFingerprint, List<BootstrapStep> steps) {

    public BootstrapPlan {
        steps = List.copyOf(steps);
    }
}
