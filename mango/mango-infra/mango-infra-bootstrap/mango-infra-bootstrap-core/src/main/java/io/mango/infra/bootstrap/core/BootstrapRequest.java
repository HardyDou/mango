package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStrategy;

public record BootstrapRequest(
        String environmentKey,
        String releaseId,
        String buildRevision,
        long generation,
        String expectedFingerprint,
        BootstrapAction action,
        BootstrapStrategy strategy,
        BootstrapPhase phase,
        int lockTimeoutSeconds) {
}
