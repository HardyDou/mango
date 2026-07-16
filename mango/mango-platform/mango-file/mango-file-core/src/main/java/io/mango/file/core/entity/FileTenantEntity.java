package io.mango.file.core.entity;

import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文件模块租户实体兼容基类。
 */
@Getter
@Setter
public abstract class FileTenantEntity extends TenantEntity {

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public void setTenantId(Long tenantId) {
        super.setTenantId(tenantId == null ? null : tenantId.toString());
    }

    public Long getTenantIdAsLong() {
        String tenantId = super.getTenantId();
        return tenantId == null || tenantId.isBlank() ? null : Long.valueOf(tenantId);
    }
}
