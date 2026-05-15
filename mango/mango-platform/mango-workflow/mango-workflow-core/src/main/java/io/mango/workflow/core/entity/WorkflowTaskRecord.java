package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流任务处理记录。
 */
@Data
@TableName("workflow_task_record")
public class WorkflowTaskRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String processInstanceId;
    private String taskId;
    private String taskName;
    private String taskDefinitionKey;
    private String action;
    private String actionName;
    private Long operatorId;
    private String operatorName;
    private String comment;
    private String variablesJson;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
