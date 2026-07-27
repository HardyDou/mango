package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapStepResult;
import io.mango.infra.bootstrap.api.BootstrapStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BootstrapOrchestratorTest {

    private final BootstrapManifestHasher hasher = new BootstrapManifestHasher();
    private final BootstrapSchemaMigrator schemaMigrator = mock(BootstrapSchemaMigrator.class);
    private final BootstrapDatabaseLock databaseLock = mock(BootstrapDatabaseLock.class);
    private final JdbcBootstrapRepository repository = mock(JdbcBootstrapRepository.class);
    private final BootstrapDatabaseLock.Lease lease = mock(BootstrapDatabaseLock.Lease.class);

    @BeforeEach
    void setUp() {
        when(databaseLock.acquire(anyString(), anyInt())).thenReturn(lease);
        when(repository.prepareCandidate(any(), anyString())).thenReturn(7L);
        when(repository.startStep(anyString(), anyString(), anyLong(), any(), anyString(), anyString()))
                .thenReturn(11L);
    }

    @Test
    void planDoesNotTouchDatabase() {
        BootstrapOrchestrator orchestrator = orchestrator(List.of());

        BootstrapOutcome outcome = orchestrator.execute(request(BootstrapAction.PLAN, BootstrapStrategy.COLD));

        assertThat(outcome.state()).isEqualTo("PLANNED");
        assertThat(outcome.executionId()).isNull();
        verifyNoInteractions(schemaMigrator, databaseLock, repository);
    }

    @Test
    void rollingApplyExecutesExpandWithoutFinalize() {
        AtomicInteger expandExecutions = new AtomicInteger();
        AtomicInteger finalizeExecutions = new AtomicInteger();
        BootstrapOrchestrator orchestrator = orchestrator(List.of(contributor(
                step("EXPAND", BootstrapPhase.EXPAND, Set.of(), expandExecutions),
                step("FINALIZE", BootstrapPhase.FINALIZE, Set.of("EXPAND"), finalizeExecutions))));
        when(repository.findControl("test")).thenReturn(java.util.Optional.of(
                new BootstrapControl("test", 0, null, 2L, "candidate", 2, "EXPANDING", 7)));

        BootstrapOutcome outcome = orchestrator.execute(request(BootstrapAction.APPLY, BootstrapStrategy.ROLLING));

        assertThat(outcome.state()).isEqualTo("EXPANDED");
        assertThat(outcome.executedSteps()).isEqualTo(1);
        assertThat(expandExecutions).hasValue(1);
        assertThat(finalizeExecutions).hasValue(0);
        verify(repository).markExpanded(anyString(), anyLong(), anyString(), anyLong());
        verify(repository, never()).beginFinalize(anyString(), anyLong(), anyString(), anyLong());
        verify(repository, never()).markFinalized(anyString(), anyLong(), anyString(), anyLong());
    }

    @Test
    void coldApplyExecutesExpandAndFinalizeInOneRun() {
        AtomicInteger expandExecutions = new AtomicInteger();
        AtomicInteger finalizeExecutions = new AtomicInteger();
        BootstrapOrchestrator orchestrator = orchestrator(List.of(contributor(
                step("EXPAND", BootstrapPhase.EXPAND, Set.of(), expandExecutions),
                step("FINALIZE", BootstrapPhase.FINALIZE, Set.of("EXPAND"), finalizeExecutions))));
        when(repository.findControl("test")).thenReturn(java.util.Optional.of(
                new BootstrapControl("test", 0, null, 2L, "candidate", 2, "EXPANDING", 7)));

        BootstrapOutcome outcome = orchestrator.execute(request(BootstrapAction.APPLY, BootstrapStrategy.COLD));

        assertThat(outcome.state()).isEqualTo("FINALIZED");
        assertThat(outcome.executedSteps()).isEqualTo(2);
        assertThat(expandExecutions).hasValue(1);
        assertThat(finalizeExecutions).hasValue(1);
        verify(repository).beginFinalize(anyString(), anyLong(), anyString(), anyLong());
        verify(repository).markFinalized(anyString(), anyLong(), anyString(), anyLong());
    }

    @Test
    void finalizeActionExecutesOnlyFinalizePhaseForPreparedCandidate() {
        AtomicInteger expandExecutions = new AtomicInteger();
        AtomicInteger finalizeExecutions = new AtomicInteger();
        BootstrapOrchestrator orchestrator = orchestrator(List.of(contributor(
                step("EXPAND", BootstrapPhase.EXPAND, Set.of(), expandExecutions),
                step("FINALIZE", BootstrapPhase.FINALIZE, Set.of("EXPAND"), finalizeExecutions))));
        String fingerprint = new BootstrapPlanBuilder(hasher)
                .build("release", "revision", List.of(contributor(
                        step("EXPAND", BootstrapPhase.EXPAND, Set.of(), new AtomicInteger()),
                        step("FINALIZE", BootstrapPhase.FINALIZE, Set.of("EXPAND"), new AtomicInteger()))))
                .manifestFingerprint();
        when(repository.findControl("test")).thenReturn(java.util.Optional.of(
                new BootstrapControl("test", 1, "stable", 2L, fingerprint, 2, "EXPANDED", 9)));

        BootstrapOutcome outcome = orchestrator.execute(request(BootstrapAction.FINALIZE, BootstrapStrategy.ROLLING));

        assertThat(outcome.state()).isEqualTo("FINALIZED");
        assertThat(outcome.executedSteps()).isEqualTo(1);
        assertThat(expandExecutions).hasValue(0);
        assertThat(finalizeExecutions).hasValue(1);
        verify(repository, never()).prepareCandidate(any(), anyString());
        verify(repository).markFinalized("test", 2L, fingerprint, 9L);
    }

    @Test
    void abortActionClearsPreparedCandidateWithoutExecutingSteps() {
        AtomicInteger expandExecutions = new AtomicInteger();
        BootstrapStepContributor contributor = contributor(
                step("EXPAND", BootstrapPhase.EXPAND, Set.of(), expandExecutions));
        BootstrapOrchestrator orchestrator = orchestrator(List.of(contributor));
        String fingerprint = new BootstrapPlanBuilder(hasher)
                .build("release", "revision", List.of(contributor))
                .manifestFingerprint();
        when(repository.findControl("test")).thenReturn(java.util.Optional.of(
                new BootstrapControl("test", 1, "stable", 2L, fingerprint, 2, "EXPANDED", 9)));
        when(repository.abortCandidate("test", 2L, fingerprint, 9L)).thenReturn(10L);

        BootstrapOutcome outcome = orchestrator.execute(request(BootstrapAction.ABORT, BootstrapStrategy.ROLLING));

        assertThat(outcome.state()).isEqualTo("ABORTED");
        assertThat(outcome.executedSteps()).isZero();
        assertThat(expandExecutions).hasValue(0);
        verify(repository).abortCandidate("test", 2L, fingerprint, 9L);
        verify(repository, never()).startStep(anyString(), anyString(), anyLong(), any(), anyString(), anyString());
    }

    @Test
    void retryReusesSucceededStepAndExecutesOnlyRemainingWork() {
        AtomicInteger expandExecutions = new AtomicInteger();
        AtomicInteger finalizeExecutions = new AtomicInteger();
        BootstrapOrchestrator orchestrator = orchestrator(List.of(contributor(
                step("EXPAND", BootstrapPhase.EXPAND, Set.of(), expandExecutions),
                step("FINALIZE", BootstrapPhase.FINALIZE, Set.of("EXPAND"), finalizeExecutions))));
        when(repository.findControl("test")).thenReturn(java.util.Optional.of(
                new BootstrapControl("test", 0, null, 2L, "candidate", 2, "FAILED", 7)));
        when(repository.stepSucceeded(eq("test"), eq(2L), eq(BootstrapPhase.EXPAND),
                eq("EXPAND"), anyString())).thenReturn(true);

        BootstrapOutcome outcome = orchestrator.execute(request(BootstrapAction.APPLY, BootstrapStrategy.COLD));

        assertThat(outcome.state()).isEqualTo("FINALIZED");
        assertThat(outcome.executedSteps()).isEqualTo(1);
        assertThat(outcome.reusedSteps()).isEqualTo(1);
        assertThat(expandExecutions).hasValue(0);
        assertThat(finalizeExecutions).hasValue(1);
    }

    @Test
    void failedStepMarksCandidateAndExecutionFailed() {
        BootstrapStep failingStep = new BootstrapStep() {
            @Override
            public String code() {
                return "FAIL";
            }

            @Override
            public BootstrapPhase phase() {
                return BootstrapPhase.EXPAND;
            }

            @Override
            public String fingerprintMaterial() {
                return "fail-v1";
            }

            @Override
            public BootstrapStepResult execute(BootstrapExecutionContext context) {
                throw new IllegalStateException("publication failed");
            }
        };
        BootstrapOrchestrator orchestrator = orchestrator(List.of(contributor(failingStep)));
        when(repository.findControl("test")).thenReturn(java.util.Optional.of(
                new BootstrapControl("test", 0, null, 2L, "candidate", 2, "EXPANDING", 7)));

        assertThatThrownBy(() -> orchestrator.execute(
                request(BootstrapAction.APPLY, BootstrapStrategy.ROLLING)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("publication failed");

        verify(repository).finishStep(eq(11L), eq("FAILED"), isNull(), any(IllegalStateException.class));
        verify(repository).markFailed("test", 2L, 7L);
        verify(repository).finishExecution(anyString(), eq("FAILED"), any(IllegalStateException.class));
    }

    private BootstrapOrchestrator orchestrator(List<BootstrapStepContributor> contributors) {
        return new BootstrapOrchestrator(new BootstrapPlanBuilder(hasher), hasher, schemaMigrator,
                databaseLock, repository, contributors);
    }

    private static BootstrapRequest request(BootstrapAction action, BootstrapStrategy strategy) {
        return new BootstrapRequest("test", "release", "revision", 2L, null,
                action, strategy, null, 5);
    }

    private static BootstrapStepContributor contributor(BootstrapStep... steps) {
        return () -> List.of(steps);
    }

    private static BootstrapStep step(String code, BootstrapPhase phase, Set<String> dependencies,
                                      AtomicInteger executions) {
        return new BootstrapStep() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public BootstrapPhase phase() {
                return phase;
            }

            @Override
            public Set<String> dependencies() {
                return dependencies;
            }

            @Override
            public String fingerprintMaterial() {
                return code + "-v1";
            }

            @Override
            public BootstrapStepResult execute(BootstrapExecutionContext context) {
                executions.incrementAndGet();
                return BootstrapStepResult.completed(code);
            }
        };
    }
}
