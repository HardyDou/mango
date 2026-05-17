package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务工作流申请当前任务。
 */
@Data
@TableName("workflow_business_apply_current_task")
public class WorkflowBusinessApplyCurrentTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long applyId;
    private String businessType;
    private String businessKey;
    private String processInstanceId;
    private String taskId;
    private String taskDefinitionKey;
    private String taskName;
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime arrivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
