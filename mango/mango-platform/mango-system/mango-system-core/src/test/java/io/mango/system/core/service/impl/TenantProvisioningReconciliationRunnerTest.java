package io.mango.system.core.service.impl;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
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

        TenantProvisioningReconciliationRunner runner = new TenantProvisioningReconciliationRunner(
                mapper,
                beans.getBeanProvider(TenantProvisioner.class),
                beans.getBeanProvider(TenantPackageBindingHandler.class));
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(calls).containsExactly("provision:2:2", "package:2:20:1");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
        assertThat(runner.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE - 40);
    }
}
