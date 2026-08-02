package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapMode;
import io.mango.infra.bootstrap.core.BootstrapOrchestrator;
import io.mango.infra.bootstrap.core.BootstrapOutcome;
import io.mango.infra.bootstrap.core.JdbcBootstrapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BootstrapCommandRunnerTest {

    private static final String FINGERPRINT = "b".repeat(64);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void writesReceiptOnlyAfterTheCommittedStableIdentityIsVerified() {
        BootstrapProperties bootstrap = bootstrapProperties(BootstrapAction.APPLY);
        MangoReleaseProperties release = releaseProperties();
        BootstrapOrchestrator orchestrator = mock(BootstrapOrchestrator.class);
        JdbcBootstrapRepository repository = mock(JdbcBootstrapRepository.class);
        when(orchestrator.execute(any())).thenReturn(
                new BootstrapOutcome("execution-1", FINGERPRINT, "FINALIZED", 3, 0));
        BootstrapReceiptWriter writer = writer(bootstrap);

        new BootstrapCommandRunner(bootstrap, release, orchestrator, repository, writer)
                .run(mock(ApplicationArguments.class));

        verify(repository).assertStableReleaseIdentity(
                "mango_023", "release-1", "revision-1", 1, FINGERPRINT);
        assertThat(temporaryDirectory.resolve("mango_023.json")).exists();
    }

    @Test
    void planNeverWritesOrVerifiesAStableReceipt() {
        BootstrapProperties bootstrap = bootstrapProperties(BootstrapAction.PLAN);
        MangoReleaseProperties release = releaseProperties();
        BootstrapOrchestrator orchestrator = mock(BootstrapOrchestrator.class);
        JdbcBootstrapRepository repository = mock(JdbcBootstrapRepository.class);
        when(orchestrator.execute(any())).thenReturn(
                new BootstrapOutcome(null, FINGERPRINT, "PLANNED", 0, 0));

        new BootstrapCommandRunner(bootstrap, release, orchestrator, repository, writer(bootstrap))
                .run(mock(ApplicationArguments.class));

        verifyNoInteractions(repository);
        assertThat(Files.exists(temporaryDirectory.resolve("mango_023.json"))).isFalse();
    }

    private BootstrapProperties bootstrapProperties(BootstrapAction action) {
        BootstrapProperties properties = new BootstrapProperties();
        properties.setMode(BootstrapMode.BOOTSTRAP);
        properties.setAction(action);
        properties.setEnvironmentKey("mango_023");
        properties.setReceiptDirectory(temporaryDirectory.toString());
        return properties;
    }

    private static MangoReleaseProperties releaseProperties() {
        MangoReleaseProperties properties = new MangoReleaseProperties();
        properties.setId("release-1");
        properties.setRevision("revision-1");
        properties.setGeneration(1);
        return properties;
    }

    private BootstrapReceiptWriter writer(BootstrapProperties properties) {
        return new BootstrapReceiptWriter(
                properties, new MockEnvironment().withProperty("MANGO_DB_NAME", "mango_dev_test"));
    }
}
