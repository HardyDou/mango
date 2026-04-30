package io.mango.infra.persistence.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 带租户字段的基础实体。
 *
 * @param <ID> 主键类型。
 */
@Getter
@Setter
public class TenantEntity<ID extends Serializable> extends AuditableEntity<ID> {

    private static final long serialVersionUID = 1L;

    /**
     * 租户标识。
     */
    private String tenantId;
}
