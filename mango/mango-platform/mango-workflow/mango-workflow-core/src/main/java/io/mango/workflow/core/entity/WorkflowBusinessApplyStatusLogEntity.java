package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 业务工作流申请状态流水。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_business_apply_status_log")
public class WorkflowBusinessApplyStatusLogEntity extends WorkflowBaseEntity {

    private Long applyId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private String actionName;
    private Long operatorId;
    private String operatorName;
    private String comment;
    private String taskId;
    private String taskDefinitionKey;
    private String processInstanceId;
}
