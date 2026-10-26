package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapStepResult;
import io.mango.infra.bootstrap.api.BootstrapStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = "mango.bootstrap.test.jdbc-url", matches = ".+")
class BootstrapOrchestratorJdbcIntegrationTest {

    private final String environmentKey = "IT_MO_" + UUID.randomUUID();
    private JdbcTemplate jdbcTemplate;
    private JdbcBootstrapRepository repository;
    private BootstrapOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                System.getProperty("mango.bootstrap.test.jdbc-url"),
                System.getProperty("mango.bootstrap.test.username", "root"),
                System.getProperty("mango.bootstrap.test.password", ""));
        new BootstrapSchemaMigrator(dataSource).migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcBootstrapRepository(jdbcTemplate);
        orchestrator = new BootstrapOrchestrator(
                new BootstrapPlanBuilder(new BootstrapManifestHasher()),
                new BootstrapManifestHasher(),
                new BootstrapSchemaMigrator(dataSource),
                new BootstrapDatabaseLock(dataSource),
                repository,
                List.of(stepContributor()));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM mango_bootstrap_step_execution WHERE environment_key = ?", environmentKey);
        jdbcTemplate.update("DELETE FROM mango_bootstrap_execution WHERE environment_key = ?", environmentKey);
        jdbcTemplate.update("DELETE FROM mango_bootstrap_control WHERE environment_key = ?", environmentKey);
    }

    @Test
    void repeatedFinalizeReturnsFinalizedWithoutTouchingControlOrExecution() {
        assertThat(orchestrator.execute(applyRequest("release-1")).state()).isEqualTo("FINALIZED");

        Map<String, Object> controlBefore = controlRow();
        long executionsBefore = countExecutions();
        long stepsBefore = countSteps();

        BootstrapOutcome repeatedOutcome = orchestrator.execute(finalizeRequest("release-1"));

        assertThat(repeatedOutcome.state()).isEqualTo("FINALIZED");
        assertThat(repeatedOutcome.executionId()).isNull();
        assertThat(repeatedOutcome.executedSteps()).isZero();
        assertThat(repeatedOutcome.reusedSteps()).isZero();
        assertThat(controlRow()).isEqualTo(controlBefore);
        assertThat(countExecutions()).isEqualTo(executionsBefore);
        assertThat(countSteps()).isEqualTo(stepsBefore);
        // The command runner asserts the stable identity right before writing the release receipt.
        repository.assertStableReleaseIdentity(environmentKey, "release-1", "revision-1", 1L,
                repeatedOutcome.manifestFingerprint());
    }

    @Test
    void repeatedFinalizeRejectsReleasedGenerationWithChangedContent() {
        assertThat(orchestrator.execute(applyRequest("release-1")).state()).isEqualTo("FINALIZED");
        Map<String, Object> controlBefore = controlRow();

        assertThatThrownBy(() -> orchestrator.execute(finalizeRequest("release-2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_GENERATION_ALREADY_RELEASED");

        assertThat(controlRow()).isEqualTo(controlBefore);
    }

    private Map<String, Object> controlRow() {
        return jdbcTemplate.queryForMap("""
                SELECT environment_key, stable_generation, stable_fingerprint, candidate_generation,
                       candidate_fingerprint, authoritative_generation, state, fencing_token,
                       release_id, build_revision, completed_at
                  FROM mango_bootstrap_control
                 WHERE environment_key = ?
                """, environmentKey);
    }

    private long countExecutions() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mango_bootstrap_execution WHERE environment_key = ?",
                Long.class, environmentKey);
        return count == null ? 0 : count;
    }

    private long countSteps() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mango_bootstrap_step_execution WHERE environment_key = ?",
                Long.class, environmentKey);
        return count == null ? 0 : count;
    }

    private BootstrapInvocation applyRequest(String releaseId) {
        return new BootstrapInvocation(environmentKey, releaseId, "revision-1", 1L, null,
                BootstrapAction.APPLY, BootstrapStrategy.COLD, BootstrapPhase.EXPAND, 5);
    }

    private BootstrapInvocation finalizeRequest(String releaseId) {
        return new BootstrapInvocation(environmentKey, releaseId, "revision-1", 1L, null,
                BootstrapAction.FINALIZE, BootstrapStrategy.ROLLING, BootstrapPhase.FINALIZE, 5);
    }

    private static BootstrapStepContributor stepContributor() {
        return () -> List.of(
                new NoOpStep("IT_EXPAND", BootstrapPhase.EXPAND),
                new NoOpStep("IT_FINALIZE", BootstrapPhase.FINALIZE));
    }

    private record NoOpStep(String code, BootstrapPhase phase) implements BootstrapStep {

        @Override
        public String fingerprintMaterial() {
            return code + "-v1";
        }

        @Override
        public BootstrapStepResult execute(BootstrapExecutionContext context) {
            return BootstrapStepResult.completed(code);
        }
    }
}
