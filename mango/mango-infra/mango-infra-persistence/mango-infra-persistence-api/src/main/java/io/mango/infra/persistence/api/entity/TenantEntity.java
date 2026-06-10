package io.mango.infra.persistence.api.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 带租户字段的基础实体。
 */
@Getter
@Setter
public class TenantEntity extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 租户标识。
     */
    private String tenantId;
}
