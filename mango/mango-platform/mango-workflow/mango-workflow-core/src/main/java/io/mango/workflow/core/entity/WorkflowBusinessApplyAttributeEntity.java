package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务申请可筛属性：一行一个键值，供任务分页按业务自定义字段精确过滤。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_business_apply_attribute")
public class WorkflowBusinessApplyAttributeEntity extends WorkflowBaseEntity {

    private Long applyId;
    private String businessType;
    private String attrKey;
    private String attrValue;
}
