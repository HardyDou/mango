package io.mango.workflow.core.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务历史查询结果行。
 */
@Data
public class WorkflowTaskHistoryRow {

    private String taskId;
    private String taskName;
    private String taskDefinitionKey;
    private String processInstanceId;
    private String assigneeKey;
    private LocalDateTime createTime;
    private LocalDateTime claimTime;
    private LocalDateTime endTime;
    private String deleteReason;
    private Long applyId;
    private String businessKey;
    private String applyStatus;
    private String processName;
    private String processKey;
    private String processDefinitionId;
    private Long orgId;
    private Boolean returned;
    private Boolean taskReturned;
    private LocalDateTime lastReturnAt;
    private String lastReturnVariables;
}
