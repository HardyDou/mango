package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务工作流申请状态流水。
 */
@Data
@TableName("workflow_business_apply_status_log")
public class WorkflowBusinessApplyStatusLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
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
    private LocalDateTime createdAt;
}
