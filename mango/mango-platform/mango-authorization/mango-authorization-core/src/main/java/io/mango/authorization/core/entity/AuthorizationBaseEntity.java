package io.mango.authorization.core.entity;

import io.mango.infra.persistence.api.entity.TenantEntity;

import java.time.LocalDateTime;

/**
 * 授权域统一实体基类。
 *
 * <p>持久化统一使用 Mango 的主键、租户和审计字段；旧业务代码中的
 * createTime/updateTime 命名仅作为 Java 兼容访问器保留，不再形成重复列。</p>
 */
public abstract class AuthorizationBaseEntity extends TenantEntity {

    private static final long serialVersionUID = 1L;

    public LocalDateTime getCreateTime() {
        return getCreatedAt();
    }

    public void setCreateTime(LocalDateTime createTime) {
        setCreatedAt(createTime);
    }

    public LocalDateTime getUpdateTime() {
        return getUpdatedAt();
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        setUpdatedAt(updateTime);
    }

    public void setTenantId(Long tenantId) {
        super.setTenantId(tenantId == null ? null : tenantId.toString());
    }

    public Long getTenantIdAsLong() {
        String tenantId = getTenantId();
        return tenantId == null || tenantId.isBlank() ? null : Long.valueOf(tenantId);
    }
}
