package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStrategy;
import io.mango.infra.bootstrap.api.BootstrapWriteAuthority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = "mango.bootstrap.test.jdbc-url", matches = ".+")
class JdbcBootstrapRepositoryIntegrationTest {

    private final String environmentKey = "IT_MB_" + UUID.randomUUID();
    private JdbcTemplate jdbcTemplate;
    private JdbcBootstrapRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                System.getProperty("mango.bootstrap.test.jdbc-url"),
                System.getProperty("mango.bootstrap.test.username", "root"),
                System.getProperty("mango.bootstrap.test.password", ""));
        new BootstrapSchemaMigrator(dataSource).migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcBootstrapRepository(jdbcTemplate);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM mango_runtime_instance WHERE environment_key = ?", environmentKey);
        jdbcTemplate.update("DELETE FROM mango_bootstrap_step_execution WHERE environment_key = ?", environmentKey);
        jdbcTemplate.update("DELETE FROM mango_bootstrap_execution WHERE environment_key = ?", environmentKey);
        jdbcTemplate.update("DELETE FROM mango_bootstrap_control WHERE environment_key = ?", environmentKey);
    }

    @Test
    void shouldFenceGenerationsAndBlockFinalizeUntilOldRuntimeDrains() {
        BootstrapInvocation generationOne = request(1);
        long firstToken = repository.prepareCandidate(generationOne, "fingerprint-one");
        repository.markExpanded(environmentKey, 1, "fingerprint-one", firstToken);
        repository.beginFinalize(environmentKey, 1, "fingerprint-one", firstToken);
        repository.markFinalized(environmentKey, 1, "fingerprint-one", firstToken);
        repository.assertRuntimeAllowed(environmentKey, 1, "fingerprint-one");
        repository.assertStableReleaseIdentity(
                environmentKey, "release-1", "revision-1", 1, "fingerprint-one");
        assertThatThrownBy(() -> repository.assertStableReleaseIdentity(
                environmentKey, "release-1", "stale-revision", 1, "fingerprint-one"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_STABLE_IDENTITY_MISMATCH");

        repository.upsertRuntimeLease("old-instance", environmentKey, "release-1", 1,
                "fingerprint-one", Duration.ofMinutes(1));
        long secondToken = repository.prepareCandidate(request(2), "fingerprint-two");
        repository.markExpanded(environmentKey, 2, "fingerprint-two", secondToken);

        repository.assertRuntimeAllowed(environmentKey, 1, "fingerprint-one");
        repository.assertRuntimeAllowed(environmentKey, 2, "fingerprint-two");
        repository.assertAuthoritative(new BootstrapWriteAuthority(
                environmentKey, 2, "fingerprint-two", secondToken));
        assertThatThrownBy(() -> repository.beginFinalize(
                environmentKey, 2, "fingerprint-two", secondToken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OLD_RUNTIME_INSTANCES_ACTIVE");

        repository.removeRuntimeLease("old-instance", environmentKey);
        repository.beginFinalize(environmentKey, 2, "fingerprint-two", secondToken);
        repository.markFinalized(environmentKey, 2, "fingerprint-two", secondToken);

        assertThatThrownBy(() -> repository.assertRuntimeAllowed(environmentKey, 1, "fingerprint-one"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STALE_RUNTIME_GENERATION");
        assertThat(repository.findControl(environmentKey)).get()
                .extracting(BootstrapControl::stableGeneration).isEqualTo(2L);
    }

    @Test
    void shouldRejectFingerprintDriftAndReturnGeneratedStepId() {
        long token = repository.prepareCandidate(request(1), "fingerprint-one");
        assertThatThrownBy(() -> repository.prepareCandidate(request(1), "different"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_FINGERPRINT_MISMATCH");

        String executionId = UUID.randomUUID().toString();
        BootstrapInvocation request = request(1);
        repository.startExecution(executionId, request, "fingerprint-one", token);
        long stepId = repository.startStep(executionId, environmentKey, 1, BootstrapPhase.EXPAND,
                "TEST_STEP", "step-fingerprint");
        repository.finishStep(stepId, "SUCCEEDED", "done", null);

        assertThat(stepId).isPositive();
        assertThat(repository.stepSucceeded(environmentKey, 1, BootstrapPhase.EXPAND,
                "TEST_STEP", "step-fingerprint")).isTrue();
    }

    @Test
    void shouldAbortCandidateOnlyAfterItsRuntimeInstancesStop() {
        long firstToken = repository.prepareCandidate(request(1), "fingerprint-one");
        repository.markExpanded(environmentKey, 1, "fingerprint-one", firstToken);
        repository.beginFinalize(environmentKey, 1, "fingerprint-one", firstToken);
        repository.markFinalized(environmentKey, 1, "fingerprint-one", firstToken);

        long secondToken = repository.prepareCandidate(request(2), "fingerprint-two");
        repository.markExpanded(environmentKey, 2, "fingerprint-two", secondToken);
        BootstrapWriteAuthority candidateAuthority = new BootstrapWriteAuthority(
                environmentKey, 2, "fingerprint-two", secondToken);
        repository.upsertRuntimeLease("candidate-instance", environmentKey, "release-2", 2,
                "fingerprint-two", Duration.ofMinutes(1));

        assertThatThrownBy(() -> repository.abortCandidate(
                environmentKey, 2, "fingerprint-two", secondToken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANDIDATE_RUNTIME_INSTANCES_ACTIVE");

        repository.removeRuntimeLease("candidate-instance", environmentKey);
        long abortToken = repository.abortCandidate(environmentKey, 2, "fingerprint-two", secondToken);

        assertThat(abortToken).isGreaterThan(secondToken);
        repository.assertRuntimeAllowed(environmentKey, 1, "fingerprint-one");
        assertThatThrownBy(() -> repository.assertRuntimeAllowed(environmentKey, 2, "fingerprint-two"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RUNTIME_GENERATION_NOT_PREPARED");
        assertThatThrownBy(() -> repository.assertAuthoritative(candidateAuthority))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESOURCE_GENERATION_FENCE_REJECTED");
        assertThat(repository.findControl(environmentKey)).get().satisfies(control -> {
            assertThat(control.stableGeneration()).isEqualTo(1L);
            assertThat(control.candidateGeneration()).isNull();
            assertThat(control.authoritativeGeneration()).isEqualTo(1L);
            assertThat(control.state()).isEqualTo("FINALIZED");
            assertThat(control.fencingToken()).isEqualTo(abortToken);
        });
    }

    @Test
    void shouldRejectAbortAfterFinalizeHasStartedAndAllowFinalizeRetry() {
        long firstToken = repository.prepareCandidate(request(1), "fingerprint-one");
        repository.markExpanded(environmentKey, 1, "fingerprint-one", firstToken);
        repository.beginFinalize(environmentKey, 1, "fingerprint-one", firstToken);
        repository.markFinalized(environmentKey, 1, "fingerprint-one", firstToken);

        long secondToken = repository.prepareCandidate(request(2), "fingerprint-two");
        repository.markExpanded(environmentKey, 2, "fingerprint-two", secondToken);
        repository.beginFinalize(environmentKey, 2, "fingerprint-two", secondToken);
        repository.markFailed(environmentKey, 2, secondToken);

        assertThat(repository.findControl(environmentKey)).get()
                .extracting(BootstrapControl::state).isEqualTo("FINALIZE_FAILED");
        assertThatThrownBy(() -> repository.abortCandidate(
                environmentKey, 2, "fingerprint-two", secondToken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_ABORT_FENCE_REJECTED");
        assertThatThrownBy(() -> repository.prepareCandidate(request(2), "fingerprint-two"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_FINALIZE_RETRY_REQUIRED");
        assertThatThrownBy(() -> repository.prepareCandidate(request(3), "fingerprint-three"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_FINALIZE_RETRY_REQUIRED");

        repository.beginFinalize(environmentKey, 2, "fingerprint-two", secondToken);
        repository.markFinalized(environmentKey, 2, "fingerprint-two", secondToken);
        assertThat(repository.findControl(environmentKey)).get()
                .extracting(BootstrapControl::stableGeneration).isEqualTo(2L);
    }

    private BootstrapInvocation request(long generation) {
        return new BootstrapInvocation(environmentKey, "release-" + generation, "revision-" + generation,
                generation, null, BootstrapAction.APPLY, BootstrapStrategy.ROLLING,
                BootstrapPhase.EXPAND, 1);
    }
}
