package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapMode;
import io.mango.infra.bootstrap.core.BootstrapPlan;
import io.mango.infra.bootstrap.core.BootstrapPlanBuilder;
import io.mango.infra.bootstrap.core.JdbcBootstrapRepository;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RuntimeLeaseManagerTest {

    private static final String FINGERPRINT = "f".repeat(64);

    @Test
    void webServerCustomizationFencesAndRegistersLeaseBeforeApplicationRunners() {
        BootstrapPlanBuilder planBuilder = mock(BootstrapPlanBuilder.class);
        JdbcBootstrapRepository repository = mock(JdbcBootstrapRepository.class);
        when(planBuilder.build("release-1", "revision-1", List.of()))
                .thenReturn(new BootstrapPlan(FINGERPRINT, List.of()));
        RuntimeLeaseManager manager = manager(planBuilder, repository, FINGERPRINT);

        manager.prepareRuntimeLease();

        var ordered = inOrder(planBuilder, repository);
        ordered.verify(planBuilder).build("release-1", "revision-1", List.of());
        ordered.verify(repository).assertRuntimeAllowed("mango_023", 2, FINGERPRINT);
        ordered.verify(repository).upsertRuntimeLease(
                "runtime-2", "mango_023", "release-1", 2, FINGERPRINT, Duration.ofSeconds(30));

        manager.run(mock(ApplicationArguments.class));
        verify(repository, times(1)).upsertRuntimeLease(
                "runtime-2", "mango_023", "release-1", 2, FINGERPRINT, Duration.ofSeconds(30));
        manager.destroy();
    }

    @Test
    void fingerprintMismatchFailsBeforeAnyRuntimeLeaseWrite() {
        BootstrapPlanBuilder planBuilder = mock(BootstrapPlanBuilder.class);
        JdbcBootstrapRepository repository = mock(JdbcBootstrapRepository.class);
        when(planBuilder.build("release-1", "revision-1", List.of()))
                .thenReturn(new BootstrapPlan(FINGERPRINT, List.of()));
        RuntimeLeaseManager manager = manager(planBuilder, repository, "0".repeat(64));

        assertThatThrownBy(manager::prepareRuntimeLease)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOTSTRAP_FINGERPRINT_MISMATCH");

        verify(repository, never()).assertRuntimeAllowed("mango_023", 2, FINGERPRINT);
        verify(repository, never()).upsertRuntimeLease(
                "runtime-2", "mango_023", "release-1", 2, FINGERPRINT, Duration.ofSeconds(30));
    }

    @Test
    void bootstrapModeNeverTouchesRuntimeFencingDuringWebServerCustomization() {
        BootstrapProperties bootstrap = bootstrapProperties();
        bootstrap.setMode(BootstrapMode.BOOTSTRAP);
        BootstrapPlanBuilder planBuilder = mock(BootstrapPlanBuilder.class);
        JdbcBootstrapRepository repository = mock(JdbcBootstrapRepository.class);
        RuntimeLeaseManager manager = new RuntimeLeaseManager(
                bootstrap, releaseProperties(FINGERPRINT), planBuilder, List.of(), repository,
                mock(ApplicationEventPublisher.class));

        manager.prepareRuntimeLease();

        verifyNoInteractions(planBuilder, repository);
    }

    @Test
    void servletInitializerPreparesLeaseOnlyAfterServletContextIsAvailable() throws Exception {
        @SuppressWarnings("unchecked")
        ObjectProvider<RuntimeLeaseManager> provider = mock(ObjectProvider.class);
        BootstrapPlanBuilder planBuilder = mock(BootstrapPlanBuilder.class);
        JdbcBootstrapRepository repository = mock(JdbcBootstrapRepository.class);
        when(planBuilder.build("release-1", "revision-1", List.of()))
                .thenReturn(new BootstrapPlan(FINGERPRINT, List.of()));
        RuntimeLeaseManager manager = manager(planBuilder, repository, FINGERPRINT);
        when(provider.getObject()).thenReturn(manager);
        RuntimeReceiptServletContextInitializer initializer =
                new RuntimeReceiptServletContextInitializer(provider);

        verifyNoInteractions(provider, planBuilder, repository);
        initializer.onStartup(mock(ServletContext.class));

        verify(provider).getObject();
        var ordered = inOrder(planBuilder, repository);
        ordered.verify(planBuilder).build("release-1", "revision-1", List.of());
        ordered.verify(repository).assertRuntimeAllowed("mango_023", 2, FINGERPRINT);
        ordered.verify(repository).upsertRuntimeLease(
                "runtime-2", "mango_023", "release-1", 2, FINGERPRINT, Duration.ofSeconds(30));
    }

    private static RuntimeLeaseManager manager(BootstrapPlanBuilder planBuilder,
                                               JdbcBootstrapRepository repository,
                                               String expectedFingerprint) {
        return new RuntimeLeaseManager(
                bootstrapProperties(), releaseProperties(expectedFingerprint), planBuilder, List.of(), repository,
                mock(ApplicationEventPublisher.class));
    }

    private static BootstrapProperties bootstrapProperties() {
        BootstrapProperties properties = new BootstrapProperties();
        properties.setMode(BootstrapMode.RUNTIME);
        properties.setEnvironmentKey("mango_023");
        properties.setInstanceId("runtime-2");
        properties.setRuntimeHeartbeatInterval(Duration.ofHours(1));
        properties.setRuntimeLeaseTtl(Duration.ofSeconds(30));
        return properties;
    }

    private static MangoReleaseProperties releaseProperties(String fingerprint) {
        MangoReleaseProperties properties = new MangoReleaseProperties();
        properties.setId("release-1");
        properties.setRevision("revision-1");
        properties.setGeneration(2);
        properties.setFingerprint(fingerprint);
        return properties;
    }
}
