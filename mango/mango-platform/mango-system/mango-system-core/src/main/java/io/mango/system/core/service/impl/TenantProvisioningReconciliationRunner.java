package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
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
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reconciles idempotent tenant baselines after Resource Registry startup sync.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantProvisioningReconciliationRunner implements ApplicationRunner, Ordered {

    private static final int ENABLED = 1;
    private static final int RESOURCE_SYNC_ORDER_OFFSET = 40;

    private final SysTenantMapper tenantMapper;
    private final ObjectProvider<TenantProvisioner> tenantProvisioners;
    private final ObjectProvider<TenantPackageBindingHandler> tenantPackageBindingHandlers;

    @Override
    public void run(ApplicationArguments args) {
        List<SysTenantEntity> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getStatus, ENABLED));
        tenants.forEach(this::reconcileTenant);
        log.info("Tenant provisioning reconciliation complete: tenants={}", tenants.size());
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
}
