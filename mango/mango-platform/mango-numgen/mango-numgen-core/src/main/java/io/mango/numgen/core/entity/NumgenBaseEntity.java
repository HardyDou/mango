package io.mango.numgen.core.entity;

import io.mango.infra.persistence.api.entity.TenantEntity;

/**
 * 编号生成域实体基类，统一使用 Mango 租户、组织和审计字段。
 */
public abstract class NumgenBaseEntity extends TenantEntity {

    private static final long serialVersionUID = 1L;
}
