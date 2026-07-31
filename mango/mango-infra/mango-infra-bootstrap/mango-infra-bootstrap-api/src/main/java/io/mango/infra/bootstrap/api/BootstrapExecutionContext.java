package io.mango.infra.bootstrap.api;

import io.mango.common.contract.LocalCapabilityContract;

@LocalCapabilityContract
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
