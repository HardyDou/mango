package io.mango.infra.bootstrap.core;

public record BootstrapOutcome(
        String executionId,
        String manifestFingerprint,
        String state,
        int executedSteps,
        int reusedSteps) {
}
