package io.mango.infra.bootstrap.starter;

import io.mango.infra.bootstrap.api.BootstrapMode;
import io.mango.infra.bootstrap.api.BootstrapRuntimeAuthorityProvider;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapWriteAuthority;
import io.mango.infra.bootstrap.core.BootstrapControl;
import io.mango.infra.bootstrap.core.BootstrapPlan;
import io.mango.infra.bootstrap.core.BootstrapPlanBuilder;
import io.mango.infra.bootstrap.core.JdbcBootstrapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class RuntimeLeaseManager implements ApplicationRunner, DisposableBean, Ordered,
        BootstrapRuntimeAuthorityProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeLeaseManager.class);

    private final BootstrapProperties bootstrapProperties;
    private final MangoReleaseProperties releaseProperties;
    private final BootstrapPlanBuilder planBuilder;
    private final List<BootstrapStepContributor> contributors;
    private final JdbcBootstrapRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private ScheduledExecutorService heartbeatExecutor;
    private String instanceId;
    private String manifestFingerprint;

    RuntimeLeaseManager(BootstrapProperties bootstrapProperties,
                        MangoReleaseProperties releaseProperties,
                        BootstrapPlanBuilder planBuilder,
                        List<BootstrapStepContributor> contributors,
                        JdbcBootstrapRepository repository,
                        ApplicationEventPublisher eventPublisher) {
        this.bootstrapProperties = bootstrapProperties;
        this.releaseProperties = releaseProperties;
        this.planBuilder = planBuilder;
        this.contributors = List.copyOf(contributors);
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapProperties.getMode() != BootstrapMode.RUNTIME) {
            return;
        }
        BootstrapPlan plan = planBuilder.build(
                releaseProperties.getId(), releaseProperties.getRevision(), contributors);
        manifestFingerprint = plan.manifestFingerprint();
        if (releaseProperties.getFingerprint() != null && !releaseProperties.getFingerprint().isBlank()
                && !releaseProperties.getFingerprint().equals(manifestFingerprint)) {
            throw new IllegalStateException("BOOTSTRAP_FINGERPRINT_MISMATCH: scope=artifact");
        }
        repository.assertRuntimeAllowed(bootstrapProperties.getEnvironmentKey(),
                releaseProperties.getGeneration(), manifestFingerprint);
        instanceId = resolveInstanceId();
        heartbeat();
        Duration interval = positive(bootstrapProperties.getRuntimeHeartbeatInterval(), Duration.ofSeconds(10));
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                .name("mango-runtime-lease-").daemon(true).factory());
        heartbeatExecutor.scheduleWithFixedDelay(this::safeHeartbeat,
                interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        LOG.info("Mango runtime receipt accepted: instanceId={}, generation={}, fingerprint={}",
                instanceId, releaseProperties.getGeneration(), manifestFingerprint);
    }

    @Override
    public void destroy() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        if (instanceId != null) {
            repository.removeRuntimeLease(instanceId, bootstrapProperties.getEnvironmentKey());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Optional<BootstrapWriteAuthority> currentWriteAuthority() {
        if (bootstrapProperties.getMode() != BootstrapMode.RUNTIME || manifestFingerprint == null) {
            return Optional.empty();
        }
        BootstrapControl control = repository.findControl(bootstrapProperties.getEnvironmentKey())
                .orElseThrow(() -> new IllegalStateException("BOOTSTRAP_RECEIPT_MISSING"));
        if (control.authoritativeGeneration() != releaseProperties.getGeneration()) {
            return Optional.empty();
        }
        repository.assertRuntimeAllowed(bootstrapProperties.getEnvironmentKey(),
                releaseProperties.getGeneration(), manifestFingerprint);
        return Optional.of(new BootstrapWriteAuthority(
                bootstrapProperties.getEnvironmentKey(), releaseProperties.getGeneration(),
                manifestFingerprint, control.fencingToken()));
    }

    private void safeHeartbeat() {
        try {
            repository.assertRuntimeAllowed(bootstrapProperties.getEnvironmentKey(),
                    releaseProperties.getGeneration(), manifestFingerprint);
            heartbeat();
        } catch (RuntimeException exception) {
            AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
            try {
                repository.removeRuntimeLease(instanceId, bootstrapProperties.getEnvironmentKey());
            } catch (RuntimeException leaseRemovalFailure) {
                exception.addSuppressed(leaseRemovalFailure);
            }
            if (heartbeatExecutor != null) {
                heartbeatExecutor.shutdown();
            }
            LOG.error("Mango runtime generation is no longer allowed: instanceId={}, generation={}, reason={}",
                    instanceId, releaseProperties.getGeneration(), exception.getMessage(), exception);
        }
    }

    private void heartbeat() {
        repository.upsertRuntimeLease(instanceId, bootstrapProperties.getEnvironmentKey(), releaseProperties.getId(),
                releaseProperties.getGeneration(), manifestFingerprint,
                positive(bootstrapProperties.getRuntimeLeaseTtl(), Duration.ofSeconds(30)));
    }

    private String resolveInstanceId() {
        if (bootstrapProperties.getInstanceId() != null && !bootstrapProperties.getInstanceId().isBlank()) {
            return bootstrapProperties.getInstanceId().trim();
        }
        return hostName() + ":" + ManagementFactory.getRuntimeMXBean().getPid() + ":" + UUID.randomUUID();
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return "unknown-host";
        }
    }

    private static Duration positive(Duration value, Duration fallback) {
        return value == null || value.isZero() || value.isNegative() ? fallback : value;
    }
}
