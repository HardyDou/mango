package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工作流任务处理记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_task_record")
public class WorkflowTaskRecordEntity extends WorkflowBaseEntity {

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
}
