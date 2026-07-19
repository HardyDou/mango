package io.mango.system.core.service.impl;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.resource.support.sync.ResourceSynchronizationCompletedEvent;
import io.mango.resource.support.sync.ResourceSynchronizationPrerequisitesReadyEvent;
import io.mango.resource.support.sync.ResourceSynchronizationStatus;
import io.mango.system.api.tenant.TenantPackageBindingHandler;
import io.mango.system.api.tenant.TenantProvisionCommand;
import io.mango.system.api.tenant.TenantProvisioner;
import io.mango.system.core.entity.SysTenantEntity;
import io.mango.system.core.mapper.SysTenantMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantProvisioningReconciliationRunnerTest {

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void reconcilesEnabledTenantsAfterResourceSyncAndRestoresContext() throws Exception {
        SysTenantEntity tenant = new SysTenantEntity();
        tenant.setId(2L);
        tenant.setTenantCode("tenant-2");
        tenant.setTenantName("租户二");
        tenant.setPackageId(20L);
        tenant.setStatus(1);
        SysTenantMapper mapper = mock(SysTenantMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(tenant));

        List<String> calls = new ArrayList<>();
        TenantProvisioner provisioner = command -> calls.add(
                "provision:" + command.getTenantId() + ":" + MangoContextHolder.tenantId());
        TenantPackageBindingHandler packageBindingHandler = (tenantId, packageId) -> calls.add(
                "package:" + tenantId + ":" + packageId + ":" + MangoContextHolder.tenantId());
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("provisioner", provisioner);
        beans.registerSingleton("packageBindingHandler", packageBindingHandler);
        beans.registerSingleton("resourceSynchronizationStatus", (ResourceSynchronizationStatus) () -> true);

        TenantProvisioningReconciliationRunner runner = new TenantProvisioningReconciliationRunner(
                mapper,
                beans.getBeanProvider(TenantProvisioner.class),
                beans.getBeanProvider(TenantPackageBindingHandler.class),
                beans.getBeanProvider(ResourceSynchronizationStatus.class),
                event -> { });
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(calls).containsExactly("provision:2:2", "package:2:20:1");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
        assertThat(runner.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE - 40);
    }

    @Test
    void bootstrapsPrerequisitesThenReconcilesAfterResourceRetryCompletes() throws Exception {
        SysTenantEntity tenant = new SysTenantEntity();
        tenant.setId(3L);
        tenant.setTenantCode("tenant-3");
        tenant.setTenantName("租户三");
        tenant.setStatus(1);
        SysTenantMapper mapper = mock(SysTenantMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(tenant));

        List<Long> reconciledTenants = new ArrayList<>();
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("provisioner", (TenantProvisioner) command ->
                reconciledTenants.add(command.getTenantId()));
        java.util.concurrent.atomic.AtomicBoolean synchronizedResources =
                new java.util.concurrent.atomic.AtomicBoolean();
        beans.registerSingleton("resourceSynchronizationStatus",
                (ResourceSynchronizationStatus) synchronizedResources::get);
        List<Object> events = new ArrayList<>();
        TenantProvisioningReconciliationRunner runner = new TenantProvisioningReconciliationRunner(
                mapper,
                beans.getBeanProvider(TenantProvisioner.class),
                beans.getBeanProvider(TenantPackageBindingHandler.class),
                beans.getBeanProvider(ResourceSynchronizationStatus.class),
                events::add);

        runner.run(new DefaultApplicationArguments(new String[0]));
        assertThat(reconciledTenants).containsExactly(3L);
        assertThat(events).singleElement().isInstanceOf(ResourceSynchronizationPrerequisitesReadyEvent.class);

        synchronizedResources.set(true);
        runner.onResourceSynchronizationCompleted(
                new ResourceSynchronizationCompletedEvent("mango-admin"));
        runner.retryUntilReconciled();

        assertThat(reconciledTenants).containsExactly(3L, 3L);
    }

    @Test
    void retriesWhenEventTriggeredReconciliationFails() {
        SysTenantEntity tenant = new SysTenantEntity();
        tenant.setId(4L);
        tenant.setTenantCode("tenant-4");
        tenant.setTenantName("租户四");
        tenant.setStatus(1);
        SysTenantMapper mapper = mock(SysTenantMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(tenant));

        AtomicInteger attempts = new AtomicInteger();
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("provisioner", (TenantProvisioner) command -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("temporary provisioning failure");
            }
        });
        beans.registerSingleton("resourceSynchronizationStatus", (ResourceSynchronizationStatus) () -> true);
        TenantProvisioningReconciliationRunner runner = new TenantProvisioningReconciliationRunner(
                mapper,
                beans.getBeanProvider(TenantProvisioner.class),
                beans.getBeanProvider(TenantPackageBindingHandler.class),
                beans.getBeanProvider(ResourceSynchronizationStatus.class),
                event -> { });

        runner.onResourceSynchronizationCompleted(
                new ResourceSynchronizationCompletedEvent("mango-admin"));
        runner.retryUntilReconciled();
        runner.retryUntilReconciled();

        assertThat(attempts).hasValue(2);
    }
}
