package io.mango.infra.bootstrap.api;

public record BootstrapExecutionContext(
        String executionId,
        String environmentKey,
        String releaseId,
        String buildRevision,
        long generation,
        String manifestFingerprint,
        long fencingToken,
        BootstrapPhase phase) {
}
