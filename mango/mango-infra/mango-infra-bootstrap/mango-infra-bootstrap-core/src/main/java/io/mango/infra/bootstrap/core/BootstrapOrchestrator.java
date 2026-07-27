package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapStepResult;
import io.mango.infra.bootstrap.api.BootstrapStrategy;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class BootstrapOrchestrator {

    private final BootstrapPlanBuilder planBuilder;
    private final BootstrapManifestHasher hasher;
    private final BootstrapSchemaMigrator schemaMigrator;
    private final BootstrapDatabaseLock databaseLock;
    private final JdbcBootstrapRepository repository;
    private final List<BootstrapStepContributor> contributors;

    public BootstrapOrchestrator(BootstrapPlanBuilder planBuilder,
                                 BootstrapManifestHasher hasher,
                                 BootstrapSchemaMigrator schemaMigrator,
                                 BootstrapDatabaseLock databaseLock,
                                 JdbcBootstrapRepository repository,
                                 List<BootstrapStepContributor> contributors) {
        this.planBuilder = Objects.requireNonNull(planBuilder, "planBuilder");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.schemaMigrator = Objects.requireNonNull(schemaMigrator, "schemaMigrator");
        this.databaseLock = Objects.requireNonNull(databaseLock, "databaseLock");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.contributors = List.copyOf(contributors);
    }

    public BootstrapOutcome execute(BootstrapRequest request) {
        validate(request);
        BootstrapPlan plan = planBuilder.build(request.releaseId(), request.buildRevision(), contributors);
        verifyExpectedFingerprint(request.expectedFingerprint(), plan.manifestFingerprint());
        if (request.action() == BootstrapAction.PLAN) {
            return new BootstrapOutcome(null, plan.manifestFingerprint(), "PLANNED", 0, 0);
        }
        if (request.action() == BootstrapAction.VERIFY) {
            repository.assertRuntimeAllowed(
                    request.environmentKey(), request.generation(), plan.manifestFingerprint());
            return new BootstrapOutcome(null, plan.manifestFingerprint(), "VERIFIED", 0, 0);
        }
        schemaMigrator.migrate();
        try (BootstrapDatabaseLock.Lease ignored = databaseLock.acquire(
                request.environmentKey(), request.lockTimeoutSeconds())) {
            if (request.action() == BootstrapAction.ABORT) {
                return abortCandidate(request, plan);
            }
            if (request.action() == BootstrapAction.FINALIZE) {
                return finalizeCandidate(request, plan);
            }
            return apply(request, plan);
        }
    }

    private BootstrapOutcome abortCandidate(BootstrapRequest request, BootstrapPlan plan) {
        BootstrapControl control = repository.findControl(request.environmentKey())
                .orElseThrow(() -> new IllegalStateException("BOOTSTRAP_RECEIPT_MISSING"));
        if (control.candidateGeneration() == null || control.candidateGeneration() != request.generation()) {
            throw new IllegalStateException("BOOTSTRAP_CANDIDATE_MISSING: generation=" + request.generation());
        }
        if (!plan.manifestFingerprint().equals(control.candidateFingerprint())) {
            throw new IllegalStateException("BOOTSTRAP_FINGERPRINT_MISMATCH: scope=abort");
        }
        String executionId = UUID.randomUUID().toString();
        long token = control.fencingToken();
        repository.startExecution(executionId, request, plan.manifestFingerprint(), token);
        try {
            repository.abortCandidate(
                    request.environmentKey(), request.generation(), plan.manifestFingerprint(), token);
            repository.finishExecution(executionId, "SUCCEEDED", null);
            return new BootstrapOutcome(executionId, plan.manifestFingerprint(), "ABORTED", 0, 0);
        } catch (RuntimeException exception) {
            repository.finishExecution(executionId, "FAILED", exception);
            throw exception;
        }
    }

    private BootstrapOutcome apply(BootstrapRequest request, BootstrapPlan plan) {
        long token = repository.prepareCandidate(request, plan.manifestFingerprint());
        BootstrapControl control = repository.findControl(request.environmentKey()).orElseThrow();
        if (control.stableGeneration() == request.generation()
                && plan.manifestFingerprint().equals(control.stableFingerprint())) {
            return new BootstrapOutcome(null, plan.manifestFingerprint(), "FINALIZED", 0, 0);
        }
        String executionId = UUID.randomUUID().toString();
        repository.startExecution(executionId, withPhase(request, BootstrapPhase.EXPAND),
                plan.manifestFingerprint(), token);
        StepCounts counts = new StepCounts();
        try {
            executePhase(executionId, request, plan, BootstrapPhase.EXPAND, token, counts);
            repository.markExpanded(request.environmentKey(), request.generation(), plan.manifestFingerprint(), token);
            if (request.strategy() == BootstrapStrategy.COLD) {
                repository.beginFinalize(
                        request.environmentKey(), request.generation(), plan.manifestFingerprint(), token);
                executePhase(executionId, request, plan, BootstrapPhase.FINALIZE, token, counts);
                repository.markFinalized(
                        request.environmentKey(), request.generation(), plan.manifestFingerprint(), token);
                repository.finishExecution(executionId, "SUCCEEDED", null);
                return new BootstrapOutcome(executionId, plan.manifestFingerprint(), "FINALIZED",
                        counts.executed, counts.reused);
            }
            repository.finishExecution(executionId, "SUCCEEDED", null);
            return new BootstrapOutcome(executionId, plan.manifestFingerprint(), "EXPANDED",
                    counts.executed, counts.reused);
        } catch (RuntimeException exception) {
            repository.markFailed(request.environmentKey(), request.generation(), token);
            repository.finishExecution(executionId, "FAILED", exception);
            throw exception;
        }
    }

    private BootstrapOutcome finalizeCandidate(BootstrapRequest request, BootstrapPlan plan) {
        BootstrapControl control = repository.findControl(request.environmentKey())
                .orElseThrow(() -> new IllegalStateException("BOOTSTRAP_RECEIPT_MISSING"));
        if (control.candidateGeneration() == null || control.candidateGeneration() != request.generation()) {
            throw new IllegalStateException("BOOTSTRAP_CANDIDATE_MISSING: generation=" + request.generation());
        }
        if (!plan.manifestFingerprint().equals(control.candidateFingerprint())) {
            throw new IllegalStateException("BOOTSTRAP_FINGERPRINT_MISMATCH: scope=finalize");
        }
        long token = control.fencingToken();
        String executionId = UUID.randomUUID().toString();
        repository.startExecution(executionId, withPhase(request, BootstrapPhase.FINALIZE),
                plan.manifestFingerprint(), token);
        StepCounts counts = new StepCounts();
        try {
            repository.beginFinalize(
                    request.environmentKey(), request.generation(), plan.manifestFingerprint(), token);
            executePhase(executionId, request, plan, BootstrapPhase.FINALIZE, token, counts);
            repository.markFinalized(
                    request.environmentKey(), request.generation(), plan.manifestFingerprint(), token);
            repository.finishExecution(executionId, "SUCCEEDED", null);
            return new BootstrapOutcome(executionId, plan.manifestFingerprint(), "FINALIZED",
                    counts.executed, counts.reused);
        } catch (RuntimeException exception) {
            repository.markFailed(request.environmentKey(), request.generation(), token);
            repository.finishExecution(executionId, "FAILED", exception);
            throw exception;
        }
    }

    private void executePhase(String executionId, BootstrapRequest request, BootstrapPlan plan,
                              BootstrapPhase phase, long token, StepCounts counts) {
        BootstrapExecutionContext context = new BootstrapExecutionContext(
                executionId, request.environmentKey(), request.releaseId(), request.buildRevision(),
                request.generation(), plan.manifestFingerprint(), token, phase);
        for (BootstrapStep step : plan.steps()) {
            if (step.phase() != phase) {
                continue;
            }
            String stepFingerprint = hasher.stepFingerprint(step);
            repository.assertNoStepDrift(
                    request.environmentKey(), request.generation(), phase, step.code(), stepFingerprint);
            if (repository.stepSucceeded(
                    request.environmentKey(), request.generation(), phase, step.code(), stepFingerprint)) {
                counts.reused++;
                continue;
            }
            long stepId = repository.startStep(executionId, request.environmentKey(), request.generation(),
                    phase, step.code(), stepFingerprint);
            try {
                BootstrapStepResult result = step.execute(context);
                repository.finishStep(stepId, "SUCCEEDED", result == null ? null : result.summary(), null);
                counts.executed++;
            } catch (RuntimeException exception) {
                repository.finishStep(stepId, "FAILED", null, exception);
                throw exception;
            }
        }
    }

    private static BootstrapRequest withPhase(BootstrapRequest request, BootstrapPhase phase) {
        return new BootstrapRequest(request.environmentKey(), request.releaseId(), request.buildRevision(),
                request.generation(), request.expectedFingerprint(), request.action(), request.strategy(),
                phase, request.lockTimeoutSeconds());
    }

    private static void verifyExpectedFingerprint(String expected, String actual) {
        if (expected != null && !expected.isBlank() && !expected.equals(actual)) {
            throw new IllegalStateException("BOOTSTRAP_FINGERPRINT_MISMATCH: scope=artifact");
        }
    }

    private static void validate(BootstrapRequest request) {
        requireText(request.environmentKey(), "Bootstrap environment key is required");
        requireText(request.releaseId(), "Mango release id is required");
        requireText(request.buildRevision(), "Mango build revision is required");
        if (request.generation() <= 0) {
            throw new IllegalStateException("Mango release generation must be positive");
        }
        Objects.requireNonNull(request.action(), "Bootstrap action is required");
        Objects.requireNonNull(request.strategy(), "Bootstrap strategy is required");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }

    private static final class StepCounts {
        private int executed;
        private int reused;
    }
}
