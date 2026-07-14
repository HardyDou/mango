package io.mango.notice.core.entity;

import io.mango.infra.persistence.api.entity.TenantEntity;

/** Notice domain base entity with canonical tenant and audit fields. */
public abstract class NoticeBaseEntity extends TenantEntity {

    private static final long serialVersionUID = 1L;
}
