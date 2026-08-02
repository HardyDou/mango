package io.mango.resource.sync.starter;

import io.mango.common.result.R;
import io.mango.infra.bootstrap.api.BootstrapRuntimeAuthorityProvider;
import io.mango.infra.bootstrap.api.BootstrapWriteAuthority;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.enums.ResourceApplyMode;
import io.mango.resource.api.enums.ResourceExecutionPhase;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.model.ResourceDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Runtime-only, non-blocking reconciliation for RUNTIME_EVENTUAL declarations. */
final class ResourceEventualReconciliationWorker implements ApplicationRunner, DisposableBean, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceEventualReconciliationWorker.class);
    private static final Duration DEFAULT_RECONCILIATION_INTERVAL = Duration.ofSeconds(30);

    private final ResourceRegistryProperties properties;
    private final ResourceDeclarationCollector collector;
    private final ResourceDeclarationApi declarationApi;
    private final ResourceManifestSerializer manifestSerializer;
    private final BootstrapRuntimeAuthorityProvider authorityProvider;
    private final String applicationName;
    private ScheduledExecutorService executor;

    ResourceEventualReconciliationWorker(ResourceRegistryProperties properties,
                                         ResourceDeclarationCollector collector,
                                         ResourceDeclarationApi declarationApi,
                                         ResourceManifestSerializer manifestSerializer,
                                         BootstrapRuntimeAuthorityProvider authorityProvider,
                                         String applicationName) {
        this.properties = properties;
        this.collector = collector;
        this.declarationApi = declarationApi;
        this.manifestSerializer = manifestSerializer;
        this.authorityProvider = authorityProvider;
        this.applicationName = applicationName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                .name("mango-resource-eventual-").daemon(true).factory());
        Duration interval = positive(properties.getEventualReconciliationInterval(),
                DEFAULT_RECONCILIATION_INTERVAL);
        executor.scheduleWithFixedDelay(this::reconcileOnce, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    void reconcileOnce() {
        try {
            BootstrapWriteAuthority authority = authorityProvider.currentWriteAuthority().orElse(null);
            if (authority == null) {
                return;
            }
            List<ResourceDeclaration> declarations = collector.collect().stream()
                    .filter(declaration -> declaration.getExecutionPhase() == ResourceExecutionPhase.RUNTIME_EVENTUAL)
                    .sorted(Comparator.comparing(ResourceDeclaration::getId))
                    .toList();
            if (declarations.isEmpty()) {
                return;
            }
            RegisterResourceDeclarationsCommand command = command(authority, declarations);
            R<Boolean> response = declarationApi.registerDeclarations(command);
            if (response == null || !response.isSuccess() || !Boolean.TRUE.equals(response.getData())) {
                throw new IllegalStateException("Resource eventual reconciliation was deferred: "
                        + (response == null ? "null response" : response.getMsg()));
            }
            LOG.debug("Mango eventual resources reconciled: generation={}, count={}",
                    authority.generation(), declarations.size());
        } catch (RuntimeException exception) {
            LOG.warn("Mango eventual resource reconciliation failed and will retry: reason={}",
                    exception.getMessage(), exception);
        }
    }

    private RegisterResourceDeclarationsCommand command(BootstrapWriteAuthority authority,
                                                         List<ResourceDeclaration> declarations) {
        RegisterResourceDeclarationsCommand command = new RegisterResourceDeclarationsCommand();
        command.setAppCode(resolveCode(properties.getRemote().getAppCode()));
        command.setServiceCode(resolveCode(properties.getRemote().getServiceCode()));
        command.setModuleCodes(collector.managedModuleCodes(declarations).stream().sorted().toList());
        command.setDeclarations(manifestSerializer.serialize(declarations));
        command.setEnvironmentKey(authority.environmentKey());
        command.setGeneration(authority.generation());
        command.setManifestFingerprint(authority.manifestFingerprint());
        command.setFencingToken(authority.fencingToken());
        command.setApplyMode(ResourceApplyMode.EVENTUAL);
        return command;
    }

    private String resolveCode(String configured) {
        if (StringUtils.hasText(configured)) {
            return configured.trim();
        }
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalStateException("Resource eventual application name is required");
        }
        return applicationName.trim();
    }

    private static Duration positive(Duration value, Duration fallback) {
        return value == null || value.isZero() || value.isNegative() ? fallback : value;
    }
}
