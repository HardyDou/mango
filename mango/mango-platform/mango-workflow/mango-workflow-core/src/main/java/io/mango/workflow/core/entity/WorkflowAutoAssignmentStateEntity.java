package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 自动派单游标。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_auto_assignment_state")
public class WorkflowAutoAssignmentStateEntity extends WorkflowBaseEntity {
    private String processDefinitionId;
    private String taskDefinitionKey;
    private Long lastAssignedUserId;
}
