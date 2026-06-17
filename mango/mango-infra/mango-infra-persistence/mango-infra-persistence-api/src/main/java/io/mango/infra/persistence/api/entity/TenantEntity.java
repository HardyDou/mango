package io.mango.infra.persistence.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /**
     * 组织标识。
     */
    @TableField(fill = FieldFill.INSERT)
    private Long orgId;
}
