package io.mango.workflow.core.entity;

import io.mango.infra.persistence.api.entity.TenantEntity;

/**
 * Workflow tenant aggregate base entity.
 */
public abstract class WorkflowBaseEntity extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Compatibility bridge for the historical numeric workflow tenant model.
     */
    public void setTenantId(Long tenantId) {
        super.setTenantId(tenantId == null ? null : String.valueOf(tenantId));
    }
}
