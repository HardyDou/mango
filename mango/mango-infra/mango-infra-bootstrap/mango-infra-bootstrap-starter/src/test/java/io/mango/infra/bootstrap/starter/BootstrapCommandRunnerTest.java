package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapMode;
import io.mango.infra.bootstrap.core.BootstrapOrchestrator;
import io.mango.infra.bootstrap.core.BootstrapOutcome;
import io.mango.infra.bootstrap.core.BootstrapSchemaMigrator;
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

        runner(bootstrap, release, orchestrator, repository, writer)
                .run(mock(ApplicationArguments.class));

        verify(repository).assertStableReleaseIdentity(
                "mango_023", "release-1", "revision-1", 1, FINGERPRINT);
        assertThat(temporaryDirectory.resolve("mango_023.json")).exists();
    }

    @Test
    void localStartupUsesLifecycleKeyAndPublishesRuntimeIdentity() {
        BootstrapProperties bootstrap = bootstrapProperties(BootstrapAction.APPLY);
        bootstrap.setLocalStartup(true);
        MangoReleaseProperties release = releaseProperties();
        BootstrapOrchestrator orchestrator = mock(BootstrapOrchestrator.class);
        BootstrapSchemaMigrator schemaMigrator = mock(BootstrapSchemaMigrator.class);
        JdbcBootstrapRepository repository = mock(JdbcBootstrapRepository.class);
        MangoLocalStartupState startupState = new MangoLocalStartupState();
        when(orchestrator.manifestFingerprint("local-mango_023-mango-backend", "revision-1"))
                .thenReturn(FINGERPRINT);
        when(orchestrator.execute(any())).thenReturn(
                new BootstrapOutcome("execution-1", FINGERPRINT, "FINALIZED", 3, 0));
        BootstrapReceiptWriter writer = writer(bootstrap);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "ignored-app")
                .withProperty("MANGO_WORKSPACE_ID", "mango_023")
                .withProperty("MANGO_LOCAL_LIFECYCLE_KEY", "mango-backend")
                .withProperty("MANGO_MAVEN_REVISION_QUALIFIER", "revision-1");

        new BootstrapCommandRunner(bootstrap, release, orchestrator, schemaMigrator, repository, writer,
                environment, startupState).run(mock(ApplicationArguments.class));

        verify(schemaMigrator).migrate();
        assertThat(startupState.requireIdentity()).isEqualTo(new MangoLocalRuntimeIdentity(
                "local-mango_023-mango-backend", "local-mango_023-mango-backend", "revision-1", 1,
                FINGERPRINT));
    }

    @Test
    void planNeverWritesOrVerifiesAStableReceipt() {
        BootstrapProperties bootstrap = bootstrapProperties(BootstrapAction.PLAN);
        MangoReleaseProperties release = releaseProperties();
        BootstrapOrchestrator orchestrator = mock(BootstrapOrchestrator.class);
        JdbcBootstrapRepository repository = mock(JdbcBootstrapRepository.class);
        when(orchestrator.execute(any())).thenReturn(
                new BootstrapOutcome(null, FINGERPRINT, "PLANNED", 0, 0));

        runner(bootstrap, release, orchestrator, repository, writer(bootstrap))
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

    private BootstrapCommandRunner runner(BootstrapProperties bootstrap,
                                          MangoReleaseProperties release,
                                          BootstrapOrchestrator orchestrator,
                                          JdbcBootstrapRepository repository,
                                          BootstrapReceiptWriter writer) {
        return new BootstrapCommandRunner(bootstrap, release, orchestrator, mock(BootstrapSchemaMigrator.class),
                repository, writer, new MockEnvironment(), null);
    }

    private BootstrapReceiptWriter writer(BootstrapProperties properties) {
        return new BootstrapReceiptWriter(
                properties, new MockEnvironment().withProperty("MANGO_DB_NAME", "mango_dev_test"));
    }
}
