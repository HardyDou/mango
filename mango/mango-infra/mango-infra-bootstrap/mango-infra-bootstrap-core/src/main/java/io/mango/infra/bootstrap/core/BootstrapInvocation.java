package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStrategy;

public final class BootstrapInvocation {

    private final String environmentKey;
    private final String releaseId;
    private final String buildRevision;
    private final long generation;
    private final String expectedFingerprint;
    private final BootstrapAction action;
    private final BootstrapStrategy strategy;
    private final BootstrapPhase phase;
    private final int lockTimeoutSeconds;

    public BootstrapInvocation(String environmentKey,
                               String releaseId,
                               String buildRevision,
                               long generation,
                               String expectedFingerprint,
                               BootstrapAction action,
                               BootstrapStrategy strategy,
                               BootstrapPhase phase,
                               int lockTimeoutSeconds) {
        this.environmentKey = environmentKey;
        this.releaseId = releaseId;
        this.buildRevision = buildRevision;
        this.generation = generation;
        this.expectedFingerprint = expectedFingerprint;
        this.action = action;
        this.strategy = strategy;
        this.phase = phase;
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    public String environmentKey() {
        return environmentKey;
    }

    public String releaseId() {
        return releaseId;
    }

    public String buildRevision() {
        return buildRevision;
    }

    public long generation() {
        return generation;
    }

    public String expectedFingerprint() {
        return expectedFingerprint;
    }

    public BootstrapAction action() {
        return action;
    }

    public BootstrapStrategy strategy() {
        return strategy;
    }

    public BootstrapPhase phase() {
        return phase;
    }

    public int lockTimeoutSeconds() {
        return lockTimeoutSeconds;
    }
}
