package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapMode;
import io.mango.infra.bootstrap.api.BootstrapStrategy;
import io.mango.infra.bootstrap.core.BootstrapControl;
import io.mango.infra.bootstrap.core.BootstrapInvocation;
import io.mango.infra.bootstrap.core.BootstrapOrchestrator;
import io.mango.infra.bootstrap.core.BootstrapOutcome;
import io.mango.infra.bootstrap.core.BootstrapSchemaMigrator;
import io.mango.infra.bootstrap.core.JdbcBootstrapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

final class BootstrapCommandRunner implements ApplicationRunner, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapCommandRunner.class);
    private static final String DEFAULT_REVISION = "local";

    private final BootstrapProperties bootstrapProperties;
    private final MangoReleaseProperties releaseProperties;
    private final BootstrapOrchestrator orchestrator;
    private final BootstrapSchemaMigrator schemaMigrator;
    private final JdbcBootstrapRepository repository;
    private final BootstrapReceiptWriter receiptWriter;
    private final Environment environment;
    private final MangoLocalStartupState localStartupState;

    BootstrapCommandRunner(BootstrapProperties bootstrapProperties,
                           MangoReleaseProperties releaseProperties,
                           BootstrapOrchestrator orchestrator,
                           BootstrapSchemaMigrator schemaMigrator,
                           JdbcBootstrapRepository repository,
                           BootstrapReceiptWriter receiptWriter,
                           Environment environment,
                           MangoLocalStartupState localStartupState) {
        this.bootstrapProperties = bootstrapProperties;
        this.releaseProperties = releaseProperties;
        this.orchestrator = orchestrator;
        this.schemaMigrator = schemaMigrator;
        this.repository = repository;
        this.receiptWriter = receiptWriter;
        this.environment = environment;
        this.localStartupState = localStartupState;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapProperties.getMode() != BootstrapMode.BOOTSTRAP) {
            return;
        }
        if (bootstrapProperties.isLocalStartup()) {
            runLocalStartup();
            return;
        }
        BootstrapOutcome outcome = orchestrator.execute(new BootstrapInvocation(
                bootstrapProperties.getEnvironmentKey(), releaseProperties.getId(), releaseProperties.getRevision(),
                releaseProperties.getGeneration(), releaseProperties.getFingerprint(),
                bootstrapProperties.getAction(), bootstrapProperties.getStrategy(), bootstrapProperties.getPhase(),
                bootstrapProperties.getLockTimeoutSeconds()));
        if ("FINALIZED".equals(outcome.state())) {
            repository.assertStableReleaseIdentity(
                    bootstrapProperties.getEnvironmentKey(), releaseProperties.getId(),
                    releaseProperties.getRevision(), releaseProperties.getGeneration(),
                    outcome.manifestFingerprint());
            receiptWriter.write(new BootstrapStableReceipt(
                    bootstrapProperties.getEnvironmentKey(), receiptWriter.databaseName(),
                    releaseProperties.getId(), releaseProperties.getRevision(), releaseProperties.getGeneration(),
                    outcome.manifestFingerprint(), outcome.state()));
        }
        logOutcome(outcome, releaseProperties.getGeneration());
    }

    private void runLocalStartup() {
        String applicationName = environment.getProperty("spring.application.name", "mango-app");
        String workspaceId = environment.getProperty("MANGO_WORKSPACE_ID", "default");
        String configuredLifecycleKey = environment.getProperty("MANGO_LOCAL_LIFECYCLE_KEY");
        String appKey = normalize(configuredLifecycleKey, normalize(applicationName, "mango-app"));
        String environmentKey = "local-" + normalize(workspaceId, "default") + "-" + appKey;
        String releaseId = environment.getProperty("mango.local.release-id", environmentKey);
        String revision = environment.getProperty("MANGO_MAVEN_REVISION_QUALIFIER", DEFAULT_REVISION);
        String fingerprint = orchestrator.manifestFingerprint(releaseId, revision);

        schemaMigrator.migrate();
        BootstrapControl control = repository.findControl(environmentKey).orElse(null);
        boolean reuseStable = control != null
                && control.stableGeneration() > 0
                && fingerprint.equals(control.stableFingerprint());
        long generation = reuseStable ? control.stableGeneration() : nextGeneration(control);
        BootstrapOutcome outcome = reuseStable
                ? new BootstrapOutcome(null, fingerprint, "FINALIZED", 0, 0)
                : orchestrator.execute(new BootstrapInvocation(
                        environmentKey, releaseId, revision, generation, null,
                        BootstrapAction.APPLY, BootstrapStrategy.COLD, bootstrapProperties.getPhase(),
                        bootstrapProperties.getLockTimeoutSeconds()));
        if (!"FINALIZED".equals(outcome.state())) {
            throw new IllegalStateException("MANGO_LOCAL_BOOTSTRAP_NOT_FINALIZED: state=" + outcome.state());
        }
        receiptWriter.write(new BootstrapStableReceipt(
                environmentKey, receiptWriter.databaseName(), releaseId, revision, generation,
                outcome.manifestFingerprint(), outcome.state()));
        if (localStartupState != null) {
            localStartupState.accept(new MangoLocalRuntimeIdentity(
                    environmentKey, releaseId, revision, generation, outcome.manifestFingerprint()));
        }
        logOutcome(outcome, generation);
    }

    private static long nextGeneration(BootstrapControl control) {
        if (control == null) {
            return 1L;
        }
        long highest = Math.max(control.stableGeneration(),
                control.candidateGeneration() == null ? 0L : control.candidateGeneration());
        return Math.addExact(highest, 1L);
    }

    private void logOutcome(BootstrapOutcome outcome, long generation) {
        LOG.info("Mango bootstrap completed: executionId={}, generation={}, fingerprint={}, state={}, "
                        + "executedSteps={}, reusedSteps={}",
                outcome.executionId(), generation, outcome.manifestFingerprint(), outcome.state(),
                outcome.executedSteps(), outcome.reusedSteps());
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? fallback : normalized;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
