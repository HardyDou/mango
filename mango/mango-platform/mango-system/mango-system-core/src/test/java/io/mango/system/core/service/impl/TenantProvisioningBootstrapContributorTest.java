package io.mango.system.core.service.impl;

import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.system.api.tenant.TenantPackageBindingHandler;
import io.mango.system.api.tenant.TenantProvisioner;
import io.mango.system.core.entity.SysTenantEntity;
import io.mango.system.core.mapper.SysTenantMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantProvisioningBootstrapContributorTest {

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void contributesPrerequisitesAndFinalReconciliationAroundRequiredResources() {
        SysTenantEntity tenant = new SysTenantEntity();
        tenant.setId(2L);
        tenant.setTenantCode("tenant-2");
        tenant.setTenantName("Tenant 2");
        tenant.setPackageId(20L);
        tenant.setStatus(1);
        SysTenantMapper mapper = mock(SysTenantMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(tenant));

        List<String> calls = new ArrayList<>();
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("tenantProvisioner", (TenantProvisioner) command ->
                calls.add("provision:" + command.getTenantId() + ":" + MangoContextHolder.tenantId()));
        beans.registerSingleton("packageBindingHandler", (TenantPackageBindingHandler) (tenantId, packageId) ->
                calls.add("bind:" + tenantId + ":" + packageId + ":" + MangoContextHolder.tenantId()));
        TenantProvisioningBootstrapContributor contributor = new TenantProvisioningBootstrapContributor(
                mapper,
                beans.getBeanProvider(TenantProvisioner.class),
                beans.getBeanProvider(TenantPackageBindingHandler.class));
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        List<BootstrapStep> steps = contributor.contributeSteps();

        assertThat(steps).extracting(BootstrapStep::code)
                .containsExactly("TENANT_PREREQUISITES", "TENANT_RECONCILIATION");
        assertThat(steps.get(0).dependencies()).containsExactly("FLYWAY_EXPAND");
        assertThat(steps.get(1).dependencies()).containsExactly("TENANT_PREREQUISITES");
        assertThat(steps.get(1).optionalDependencies()).containsExactly("RESOURCE_REQUIRED");

        steps.get(0).execute(context());
        steps.get(1).execute(context());

        assertThat(calls).containsExactly(
                "provision:2:2", "bind:2:20:1",
                "provision:2:2", "bind:2:20:1");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    private static BootstrapExecutionContext context() {
        return new BootstrapExecutionContext("execution", "test", "release", "revision",
                1L, "f".repeat(64), 1L, BootstrapPhase.EXPAND);
    }
}
