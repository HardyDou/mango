package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.infra.bootstrap.api.BootstrapExecutionContext;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapStepResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.system.api.tenant.TenantPackageBindingHandler;
import io.mango.system.api.tenant.TenantProvisionCommand;
import io.mango.system.api.tenant.TenantProvisioner;
import io.mango.system.core.entity.SysTenantEntity;
import io.mango.system.core.mapper.SysTenantMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public final class TenantProvisioningBootstrapContributor implements BootstrapStepContributor {

    private static final int ENABLED = 1;

    private final SysTenantMapper tenantMapper;
    private final ObjectProvider<TenantProvisioner> tenantProvisioners;
    private final ObjectProvider<TenantPackageBindingHandler> tenantPackageBindingHandlers;

    public TenantProvisioningBootstrapContributor(
            SysTenantMapper tenantMapper,
            ObjectProvider<TenantProvisioner> tenantProvisioners,
            ObjectProvider<TenantPackageBindingHandler> tenantPackageBindingHandlers) {
        this.tenantMapper = tenantMapper;
        this.tenantProvisioners = tenantProvisioners;
        this.tenantPackageBindingHandlers = tenantPackageBindingHandlers;
    }

    @Override
    public List<BootstrapStep> contributeSteps() {
        return List.of(new TenantStep("TENANT_PREREQUISITES", Set.of("FLYWAY_EXPAND"), Set.of()),
                new TenantStep("TENANT_RECONCILIATION", Set.of("TENANT_PREREQUISITES"),
                        Set.of("RESOURCE_REQUIRED")));
    }

    private String fingerprintMaterial(String code) {
        List<String> provisioners = tenantProvisioners.orderedStream()
                .map(value -> value.getClass().getName()).sorted().toList();
        List<String> bindings = tenantPackageBindingHandlers.orderedStream()
                .map(value -> value.getClass().getName()).sorted().toList();
        return "tenant-provisioning-v1|" + code + "|provisioners=" + provisioners + "|bindings=" + bindings;
    }

    private int reconcileTenants() {
        List<SysTenantEntity> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getStatus, ENABLED));
        tenants.forEach(this::reconcileTenant);
        return tenants.size();
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

    private final class TenantStep implements BootstrapStep {

        private final String code;
        private final Set<String> dependencies;
        private final Set<String> optionalDependencies;

        private TenantStep(String code, Set<String> dependencies, Set<String> optionalDependencies) {
            this.code = code;
            this.dependencies = dependencies;
            this.optionalDependencies = optionalDependencies;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public BootstrapPhase phase() {
            return BootstrapPhase.EXPAND;
        }

        @Override
        public Set<String> dependencies() {
            return dependencies;
        }

        @Override
        public Set<String> optionalDependencies() {
            return optionalDependencies;
        }

        @Override
        public String fingerprintMaterial() {
            return TenantProvisioningBootstrapContributor.this.fingerprintMaterial(code);
        }

        @Override
        public BootstrapStepResult execute(BootstrapExecutionContext context) {
            int tenantCount = reconcileTenants();
            return new BootstrapStepResult(code + " completed", Map.of("tenants", tenantCount));
        }
    }
}
