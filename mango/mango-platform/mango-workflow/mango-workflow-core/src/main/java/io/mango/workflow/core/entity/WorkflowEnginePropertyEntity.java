package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Flowable 引擎属性实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ACT_GE_PROPERTY", excludeProperty = {
        "id", "tenantId", "orgId", "createdBy", "createdAt", "updatedBy", "updatedAt"
})
public class WorkflowEnginePropertyEntity extends TenantEntity {

    @TableId(value = "NAME_", type = IdType.INPUT)
    private String name;

    @TableField("VALUE_")
    private String value;

    @TableField("REV_")
    private Integer revision;
}
