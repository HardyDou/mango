package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 业务工作流申请当前任务。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_business_apply_current_task")
public class WorkflowBusinessApplyCurrentTaskEntity extends WorkflowBaseEntity {

    private Long applyId;
    private String businessType;
    private String businessKey;
    private String processInstanceId;
    private String taskId;
    private String taskDefinitionKey;
    private String taskName;
    private Long assigneeId;
    private String assigneeName;
    private String claimStatus;
    private String candidateUsers;
    private String candidateGroups;
    private LocalDateTime arrivedAt;
}
