package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.resource.support.sync.ResourceSynchronizationCompletedEvent;
import io.mango.resource.support.sync.ResourceSynchronizationPrerequisitesReadyEvent;
import io.mango.resource.support.sync.ResourceSynchronizationStatus;
import io.mango.resource.support.sync.StartupReadinessChangedEvent;
import io.mango.resource.support.sync.StartupReadinessState;
import io.mango.resource.support.sync.StartupReadinessStatus;
import io.mango.system.api.tenant.TenantPackageBindingHandler;
import io.mango.system.api.tenant.TenantProvisionCommand;
import io.mango.system.api.tenant.TenantProvisioner;
import io.mango.system.core.entity.SysTenantEntity;
import io.mango.system.core.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reconciles idempotent tenant baselines after Resource Registry startup sync.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantProvisioningReconciliationRunner
        implements ApplicationRunner, Ordered, StartupReadinessStatus {

    private static final int ENABLED = 1;
    private static final int RESOURCE_SYNC_ORDER_OFFSET = 40;

    private final SysTenantMapper tenantMapper;
    private final ObjectProvider<TenantProvisioner> tenantProvisioners;
    private final ObjectProvider<TenantPackageBindingHandler> tenantPackageBindingHandlers;
    private final ObjectProvider<ResourceSynchronizationStatus> resourceSynchronizationStatuses;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean reconciliationCompleted = new AtomicBoolean();
    private final AtomicBoolean reconciliationInProgress = new AtomicBoolean();
    private final AtomicReference<StartupReadinessState> readinessState =
            new AtomicReference<>(StartupReadinessState.RECONCILING_TENANTS);
    private final AtomicInteger failureCount = new AtomicInteger();
    private volatile long lastAttemptAtMillis;
    private volatile long lastFailureAtMillis;
    private volatile String lastErrorType;

    @Override
    public void run(ApplicationArguments args) {
        if (!isResourceSynchronizationComplete()) {
            reconcilePrerequisites();
            eventPublisher.publishEvent(new ResourceSynchronizationPrerequisitesReadyEvent());
            if (!isResourceSynchronizationComplete()) {
                transitionTo(StartupReadinessState.TRANSIENT_WAIT);
                log.warn("Tenant provisioning final reconciliation deferred until resource synchronization completes");
                return;
            }
        }
        tryReconcile("application startup");
    }

    /**
     * Reconciles tenants after a deferred resource synchronization eventually succeeds.
     *
     * @param event resource synchronization completion event
     */
    @EventListener
    public void onResourceSynchronizationCompleted(ResourceSynchronizationCompletedEvent event) {
        tryReconcile("resource synchronization event for " + event.getApplicationName());
    }

    /**
     * Retries tenant reconciliation after a transient provisioning failure.
     */
    @Scheduled(
            fixedDelayString = "${mango.system.tenant-provisioning.retry-interval:10s}",
            initialDelayString = "${mango.system.tenant-provisioning.retry-interval:10s}")
    public void retryUntilReconciled() {
        if (!reconciliationCompleted.get() && isResourceSynchronizationComplete()) {
            tryReconcile("scheduled retry");
        }
    }

    private void tryReconcile(String trigger) {
        lastAttemptAtMillis = System.currentTimeMillis();
        try {
            reconcileTenants();
        } catch (RuntimeException exception) {
            failureCount.incrementAndGet();
            lastFailureAtMillis = System.currentTimeMillis();
            lastErrorType = exception.getClass().getSimpleName();
            transitionTo(StartupReadinessState.TRANSIENT_WAIT);
            log.error("Tenant provisioning reconciliation failed and will retry: trigger={}", trigger, exception);
        }
    }

    private void reconcileTenants() {
        if (reconciliationCompleted.get() || !reconciliationInProgress.compareAndSet(false, true)) {
            return;
        }
        transitionTo(StartupReadinessState.RECONCILING_TENANTS);
        try {
            int tenantCount = doReconcileTenants();
            reconciliationCompleted.set(true);
            failureCount.set(0);
            lastFailureAtMillis = 0L;
            lastErrorType = null;
            transitionTo(StartupReadinessState.READY);
            log.info("Tenant provisioning reconciliation complete: tenants={}", tenantCount);
        } finally {
            reconciliationInProgress.set(false);
        }
    }

    private void reconcilePrerequisites() {
        if (reconciliationCompleted.get() || !reconciliationInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            int tenantCount = doReconcileTenants();
            log.info("Tenant provisioning prerequisites ready: tenants={}", tenantCount);
        } finally {
            reconciliationInProgress.set(false);
        }
    }

    private int doReconcileTenants() {
        List<SysTenantEntity> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getStatus, ENABLED));
        tenants.forEach(this::reconcileTenant);
        return tenants.size();
    }

    private boolean isResourceSynchronizationComplete() {
        return resourceSynchronizationStatuses.orderedStream()
                .findFirst()
                .map(ResourceSynchronizationStatus::isSynchronizationComplete)
                .orElse(true);
    }

    private void reconcileTenant(SysTenantEntity tenant) {
        TenantProvisionCommand command = new TenantProvisionCommand(
                tenant.getId(), tenant.getTenantCode(), tenant.getTenantName());
        MangoContextSnapshot original = MangoContextHolder.get();
        MangoContextHolder.set(original.withTenantId(String.valueOf(tenant.getId())));
        try {
            tenantProvisioners.orderedStream().forEach(provisioner -> provisioner.provision(command));
        } finally {
            MangoContextHolder.set(original);
        }
        if (tenant.getPackageId() != null) {
            tenantPackageBindingHandlers.orderedStream()
                    .forEach(handler -> handler.bindPackage(tenant.getId(), tenant.getPackageId()));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - RESOURCE_SYNC_ORDER_OFFSET;
    }

    @Override
    public String getReadinessComponent() {
        return "tenant-provisioning-reconciliation";
    }

    @Override
    public StartupReadinessState getReadinessState() {
        return readinessState.get();
    }

    @Override
    public Map<String, Object> getReadinessDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("state", getReadinessState().name());
        details.put("failureCount", failureCount.get());
        putTimestamp(details, "lastAttemptAt", lastAttemptAtMillis);
        putTimestamp(details, "lastFailureAt", lastFailureAtMillis);
        if (lastErrorType != null) {
            details.put("lastErrorType", lastErrorType);
        }
        return details;
    }

    private void putTimestamp(Map<String, Object> details, String name, long epochMillis) {
        if (epochMillis > 0L) {
            details.put(name, Instant.ofEpochMilli(epochMillis).toString());
        }
    }

    private void transitionTo(StartupReadinessState nextState) {
        StartupReadinessState previous = readinessState.getAndSet(nextState);
        if (previous != nextState) {
            eventPublisher.publishEvent(new StartupReadinessChangedEvent(getReadinessComponent(), nextState));
        }
    }
}
