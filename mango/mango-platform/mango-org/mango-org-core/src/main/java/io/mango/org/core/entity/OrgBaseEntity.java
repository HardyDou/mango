package io.mango.org.core.entity;

import io.mango.infra.persistence.api.entity.TenantEntity;

import java.time.LocalDateTime;

/**
 * 组织域实体公共基类，统一租户、组织和审计字段。
 */
public abstract class OrgBaseEntity extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 兼容组织域历史数字租户标识写入方式。
     *
     * @param tenantId 数字租户标识
     */
    public void setTenantId(Long tenantId) {
        super.setTenantId(tenantId == null ? null : tenantId.toString());
    }

    /**
     * 将标准字符串租户标识转换为历史数字租户标识。
     *
     * @return 数字租户标识
     */
    public Long getTenantIdAsLong() {
        String tenantId = getTenantId();
        return tenantId == null || tenantId.isBlank() ? null : Long.valueOf(tenantId);
    }

    /**
     * 历史创建时间访问器。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return getCreatedAt();
    }

    /**
     * 历史创建时间写入器。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        setCreatedAt(createTime);
    }

    /**
     * 历史更新时间访问器。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return getUpdatedAt();
    }

    /**
     * 历史更新时间写入器。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        setUpdatedAt(updateTime);
    }
}
