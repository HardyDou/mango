package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapGenerationFence;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapWriteAuthority;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcBootstrapRepository implements BootstrapGenerationFence {

    private static final int MAX_SUMMARY_LENGTH = 1000;

    private final JdbcTemplate jdbcTemplate;

    public JdbcBootstrapRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    public Optional<BootstrapControl> findControl(String environmentKey) {
        try {
            List<BootstrapControl> rows = jdbcTemplate.query("""
                            SELECT environment_key, stable_generation, stable_fingerprint,
                                   candidate_generation, candidate_fingerprint,
                                   authoritative_generation, state, fencing_token
                              FROM mango_bootstrap_control
                             WHERE environment_key = ?
                            """,
                    (result, row) -> new BootstrapControl(
                            result.getString("environment_key"),
                            result.getLong("stable_generation"),
                            result.getString("stable_fingerprint"),
                            nullableLong(result, "candidate_generation"),
                            result.getString("candidate_fingerprint"),
                            result.getLong("authoritative_generation"),
                            result.getString("state"),
                            result.getLong("fencing_token")),
                    environmentKey);
            return rows.stream().findFirst();
        } catch (DataAccessException exception) {
            throw new IllegalStateException("BOOTSTRAP_SCHEMA_UNAVAILABLE", exception);
        }
    }

    public long prepareCandidate(BootstrapInvocation request, String fingerprint) {
        Optional<BootstrapControl> existing = findControl(request.environmentKey());
        if (existing.isEmpty()) {
            jdbcTemplate.update("""
                            INSERT INTO mango_bootstrap_control
                            (environment_key, stable_generation, candidate_generation, candidate_fingerprint,
                             authoritative_generation, state, fencing_token, release_id, build_revision, started_at)
                            VALUES (?, 0, ?, ?, ?, 'EXPANDING', 1, ?, ?, CURRENT_TIMESTAMP(6))
                            """,
                    request.environmentKey(), request.generation(), fingerprint, request.generation(),
                    request.releaseId(), request.buildRevision());
            return 1L;
        }
        BootstrapControl control = existing.get();
        long highest = Math.max(control.stableGeneration(),
                control.candidateGeneration() == null ? 0 : control.candidateGeneration());
        if (request.generation() < highest) {
            throw new IllegalStateException("STALE_BOOTSTRAP_GENERATION: requested=" + request.generation()
                    + ", highest=" + highest);
        }
        if (request.generation() == control.stableGeneration()) {
            requireFingerprint(control.stableFingerprint(), fingerprint, "stable");
            return control.fencingToken();
        }
        if ("FINALIZING".equals(control.state()) || "FINALIZE_FAILED".equals(control.state())) {
            throw new IllegalStateException(
                    "BOOTSTRAP_FINALIZE_RETRY_REQUIRED: generation=" + control.candidateGeneration());
        }
        if (control.candidateGeneration() != null && request.generation() == control.candidateGeneration()) {
            requireFingerprint(control.candidateFingerprint(), fingerprint, "candidate");
        } else if (request.generation() == highest) {
            throw new IllegalStateException("BOOTSTRAP_GENERATION_CONFLICT: generation=" + request.generation());
        }
        long token = Math.addExact(control.fencingToken(), 1L);
        int updated = jdbcTemplate.update("""
                        UPDATE mango_bootstrap_control
                           SET candidate_generation = ?, candidate_fingerprint = ?, authoritative_generation = ?,
                               state = 'EXPANDING', fencing_token = ?, release_id = ?, build_revision = ?,
                               started_at = CURRENT_TIMESTAMP(6), completed_at = NULL
                         WHERE environment_key = ? AND fencing_token = ?
                        """,
                request.generation(), fingerprint, request.generation(), token,
                request.releaseId(), request.buildRevision(), request.environmentKey(), control.fencingToken());
        requireUpdated(updated, "BOOTSTRAP_CANDIDATE_FENCE_REJECTED");
        return token;
    }

    public void markExpanded(String environmentKey, long generation, String fingerprint, long token) {
        int updated = jdbcTemplate.update("""
                        UPDATE mango_bootstrap_control
                           SET state = 'EXPANDED', completed_at = CURRENT_TIMESTAMP(6)
                         WHERE environment_key = ? AND candidate_generation = ?
                           AND candidate_fingerprint = ? AND fencing_token = ?
                        """, environmentKey, generation, fingerprint, token);
        requireUpdated(updated, "BOOTSTRAP_EXPAND_FENCE_REJECTED");
    }

    public void beginFinalize(String environmentKey, long generation, String fingerprint, long token) {
        int activeOld = countActiveOlderInstances(environmentKey, generation);
        if (activeOld > 0) {
            throw new IllegalStateException("OLD_RUNTIME_INSTANCES_ACTIVE: count=" + activeOld);
        }
        int updated = jdbcTemplate.update("""
                        UPDATE mango_bootstrap_control
                           SET state = 'FINALIZING'
                         WHERE environment_key = ? AND candidate_generation = ?
                           AND candidate_fingerprint = ? AND fencing_token = ?
                           AND state IN ('EXPANDED', 'FINALIZING', 'FINALIZE_FAILED')
                        """, environmentKey, generation, fingerprint, token);
        requireUpdated(updated, "BOOTSTRAP_FINALIZE_FENCE_REJECTED");
    }

    public void markFinalized(String environmentKey, long generation, String fingerprint, long token) {
        int updated = jdbcTemplate.update("""
                        UPDATE mango_bootstrap_control
                           SET stable_generation = ?, stable_fingerprint = ?,
                               candidate_generation = NULL, candidate_fingerprint = NULL,
                               authoritative_generation = ?, state = 'FINALIZED',
                               completed_at = CURRENT_TIMESTAMP(6)
                         WHERE environment_key = ? AND candidate_generation = ?
                           AND candidate_fingerprint = ? AND fencing_token = ?
                        """, generation, fingerprint, generation,
                environmentKey, generation, fingerprint, token);
        requireUpdated(updated, "BOOTSTRAP_FINALIZE_COMMIT_REJECTED");
    }

    public long abortCandidate(String environmentKey, long generation, String fingerprint, long token) {
        int activeCandidate = countActiveGenerationInstances(environmentKey, generation);
        if (activeCandidate > 0) {
            throw new IllegalStateException("CANDIDATE_RUNTIME_INSTANCES_ACTIVE: count=" + activeCandidate);
        }
        long nextToken = Math.addExact(token, 1L);
        int updated = jdbcTemplate.update("""
                        UPDATE mango_bootstrap_control
                           SET candidate_generation = NULL, candidate_fingerprint = NULL,
                               authoritative_generation = stable_generation,
                               state = CASE WHEN stable_generation > 0 THEN 'FINALIZED' ELSE 'EMPTY' END,
                               fencing_token = ?, completed_at = CURRENT_TIMESTAMP(6)
                         WHERE environment_key = ? AND candidate_generation = ?
                           AND candidate_fingerprint = ? AND fencing_token = ?
                           AND state IN ('EXPANDING', 'EXPANDED', 'FAILED')
                        """, nextToken, environmentKey, generation, fingerprint, token);
        requireUpdated(updated, "BOOTSTRAP_ABORT_FENCE_REJECTED");
        return nextToken;
    }

    public void markFailed(String environmentKey, long generation, long token) {
        jdbcTemplate.update("""
                        UPDATE mango_bootstrap_control
                           SET state = CASE WHEN state = 'FINALIZING' THEN 'FINALIZE_FAILED' ELSE 'FAILED' END
                         WHERE environment_key = ? AND candidate_generation = ? AND fencing_token = ?
                        """, environmentKey, generation, token);
    }

    public void assertRuntimeAllowed(String environmentKey, long generation, String fingerprint) {
        BootstrapControl control = findControl(environmentKey)
                .orElseThrow(() -> new IllegalStateException("BOOTSTRAP_RECEIPT_MISSING"));
        if (generation == control.stableGeneration()) {
            requireFingerprint(control.stableFingerprint(), fingerprint, "runtime-stable");
            return;
        }
        if (control.candidateGeneration() != null
                && generation == control.candidateGeneration()
                && ("EXPANDED".equals(control.state()) || "FINALIZING".equals(control.state()))) {
            requireFingerprint(control.candidateFingerprint(), fingerprint, "runtime-candidate");
            return;
        }
        if (generation < control.stableGeneration()) {
            throw new IllegalStateException("STALE_RUNTIME_GENERATION: requested=" + generation
                    + ", stable=" + control.stableGeneration());
        }
        throw new IllegalStateException("RUNTIME_GENERATION_NOT_PREPARED: requested=" + generation
                + ", state=" + control.state());
    }

    @Override
    public void assertAuthoritative(BootstrapWriteAuthority authority) {
        BootstrapControl control = findControl(authority.environmentKey())
                .orElseThrow(() -> new IllegalStateException("BOOTSTRAP_RECEIPT_MISSING"));
        if (control.authoritativeGeneration() != authority.generation()
                || control.fencingToken() != authority.fencingToken()) {
            throw new IllegalStateException("RESOURCE_GENERATION_FENCE_REJECTED");
        }
        String expected = control.candidateGeneration() != null
                && control.candidateGeneration() == authority.generation()
                ? control.candidateFingerprint() : control.stableFingerprint();
        requireFingerprint(expected, authority.manifestFingerprint(), "resource-authority");
    }

    public void startExecution(String executionId, BootstrapInvocation request, String fingerprint, long token) {
        jdbcTemplate.update("""
                        INSERT INTO mango_bootstrap_execution
                        (execution_id, environment_key, release_id, build_revision, generation,
                         manifest_fingerprint, action, phase, status, fencing_token)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'RUNNING', ?)
                        """, executionId, request.environmentKey(), request.releaseId(), request.buildRevision(),
                request.generation(), fingerprint, request.action().name(),
                request.phase() == null ? null : request.phase().name(), token);
    }

    public void finishExecution(String executionId, String status, Throwable failure) {
        jdbcTemplate.update("""
                        UPDATE mango_bootstrap_execution
                           SET status = ?, error_type = ?, error_summary = ?, completed_at = CURRENT_TIMESTAMP(6)
                         WHERE execution_id = ?
                        """, status,
                failure == null ? null : failure.getClass().getName(),
                failure == null ? null : summarize(failure.getMessage()), executionId);
    }

    public boolean stepSucceeded(String environmentKey, long generation, BootstrapPhase phase,
                                 String stepCode, String stepFingerprint) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM mango_bootstrap_step_execution
                         WHERE environment_key = ? AND generation = ? AND phase = ?
                           AND step_code = ? AND step_fingerprint = ? AND status = 'SUCCEEDED'
                        """, Integer.class, environmentKey, generation, phase.name(), stepCode, stepFingerprint);
        return count != null && count > 0;
    }

    public void assertNoStepDrift(String environmentKey, long generation, BootstrapPhase phase,
                                  String stepCode, String stepFingerprint) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM mango_bootstrap_step_execution
                         WHERE environment_key = ? AND generation = ? AND phase = ?
                           AND step_code = ? AND step_fingerprint <> ? AND status = 'SUCCEEDED'
                        """, Integer.class, environmentKey, generation, phase.name(), stepCode, stepFingerprint);
        if (count != null && count > 0) {
            throw new IllegalStateException("BOOTSTRAP_STEP_FINGERPRINT_DRIFT: step=" + stepCode);
        }
    }

    public long startStep(String executionId, String environmentKey, long generation, BootstrapPhase phase,
                          String stepCode, String stepFingerprint) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO mango_bootstrap_step_execution
                    (execution_id, environment_key, generation, phase, step_code, step_fingerprint, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'RUNNING')
                    """, Statement.RETURN_GENERATED_KEYS);
            try {
                int parameterIndex = 1;
                statement.setString(parameterIndex++, executionId);
                statement.setString(parameterIndex++, environmentKey);
                statement.setLong(parameterIndex++, generation);
                statement.setString(parameterIndex++, phase.name());
                statement.setString(parameterIndex++, stepCode);
                statement.setString(parameterIndex, stepFingerprint);
                return statement;
            } catch (SQLException exception) {
                closeAfterPreparationFailure(statement, exception);
                throw exception;
            }
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("BOOTSTRAP_STEP_ID_UNAVAILABLE");
        }
        return id.longValue();
    }

    public void finishStep(long id, String status, String summary, Throwable failure) {
        jdbcTemplate.update("""
                        UPDATE mango_bootstrap_step_execution
                           SET status = ?, result_summary = ?, error_type = ?, error_summary = ?,
                               completed_at = CURRENT_TIMESTAMP(6)
                         WHERE id = ?
                        """, status, summarize(summary),
                failure == null ? null : failure.getClass().getName(),
                failure == null ? null : summarize(failure.getMessage()), id);
    }

    public void upsertRuntimeLease(String instanceId, String environmentKey, String releaseId,
                                   long generation, String fingerprint, Duration ttl) {
        Instant expiry = Instant.now().plus(ttl);
        int updated = jdbcTemplate.update("""
                        UPDATE mango_runtime_instance
                           SET release_id = ?, generation = ?, manifest_fingerprint = ?, draining = 0,
                               last_heartbeat_at = CURRENT_TIMESTAMP(6), lease_expires_at = ?
                         WHERE instance_id = ? AND environment_key = ?
                        """, releaseId, generation, fingerprint, Timestamp.from(expiry), instanceId, environmentKey);
        if (updated == 0) {
            jdbcTemplate.update("""
                            INSERT INTO mango_runtime_instance
                            (instance_id, environment_key, release_id, generation, manifest_fingerprint,
                             draining, lease_expires_at)
                            VALUES (?, ?, ?, ?, ?, 0, ?)
                            """, instanceId, environmentKey, releaseId, generation, fingerprint,
                    Timestamp.from(expiry));
        }
    }

    public void removeRuntimeLease(String instanceId, String environmentKey) {
        jdbcTemplate.update("DELETE FROM mango_runtime_instance WHERE instance_id = ? AND environment_key = ?",
                instanceId, environmentKey);
    }

    public int countActiveOlderInstances(String environmentKey, long generation) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM mango_runtime_instance
                         WHERE environment_key = ? AND generation < ?
                           AND lease_expires_at > CURRENT_TIMESTAMP(6)
                        """, Integer.class, environmentKey, generation);
        return count == null ? 0 : count;
    }

    private int countActiveGenerationInstances(String environmentKey, long generation) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM mango_runtime_instance
                         WHERE environment_key = ? AND generation = ?
                           AND lease_expires_at > CURRENT_TIMESTAMP(6)
                        """, Integer.class, environmentKey, generation);
        return count == null ? 0 : count;
    }

    private static Long nullableLong(java.sql.ResultSet result, String column) throws java.sql.SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static void requireFingerprint(String expected, String actual, String scope) {
        if (expected == null || !expected.equals(actual)) {
            throw new IllegalStateException("BOOTSTRAP_FINGERPRINT_MISMATCH: scope=" + scope);
        }
    }

    private static void requireUpdated(int updated, String reason) {
        if (updated != 1) {
            throw new IllegalStateException(reason);
        }
    }

    private static String summarize(String value) {
        if (value == null || value.length() <= MAX_SUMMARY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_SUMMARY_LENGTH);
    }

    private static void closeAfterPreparationFailure(PreparedStatement statement, SQLException failure) {
        try {
            statement.close();
        } catch (SQLException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
