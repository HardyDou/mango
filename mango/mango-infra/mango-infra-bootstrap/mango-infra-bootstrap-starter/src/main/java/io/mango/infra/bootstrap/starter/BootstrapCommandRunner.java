package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapMode;
import io.mango.infra.bootstrap.core.BootstrapOrchestrator;
import io.mango.infra.bootstrap.core.BootstrapOutcome;
import io.mango.infra.bootstrap.core.BootstrapRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

final class BootstrapCommandRunner implements ApplicationRunner, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapCommandRunner.class);

    private final BootstrapProperties bootstrapProperties;
    private final MangoReleaseProperties releaseProperties;
    private final BootstrapOrchestrator orchestrator;

    BootstrapCommandRunner(BootstrapProperties bootstrapProperties,
                           MangoReleaseProperties releaseProperties,
                           BootstrapOrchestrator orchestrator) {
        this.bootstrapProperties = bootstrapProperties;
        this.releaseProperties = releaseProperties;
        this.orchestrator = orchestrator;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapProperties.getMode() != BootstrapMode.BOOTSTRAP) {
            return;
        }
        BootstrapOutcome outcome = orchestrator.execute(new BootstrapRequest(
                bootstrapProperties.getEnvironmentKey(), releaseProperties.getId(), releaseProperties.getRevision(),
                releaseProperties.getGeneration(), releaseProperties.getFingerprint(),
                bootstrapProperties.getAction(), bootstrapProperties.getStrategy(), bootstrapProperties.getPhase(),
                bootstrapProperties.getLockTimeoutSeconds()));
        LOG.info("Mango bootstrap completed: executionId={}, generation={}, fingerprint={}, state={}, "
                        + "executedSteps={}, reusedSteps={}",
                outcome.executionId(), releaseProperties.getGeneration(), outcome.manifestFingerprint(),
                outcome.state(), outcome.executedSteps(), outcome.reusedSteps());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
