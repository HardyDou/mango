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

    /** 业务应用任务快照里的承办人来源；仅在该在途任务仍存在于快照中时有值。 */
    private String assignSource;

    /** 业务应用任务快照里的领取时间；引擎 CLAIM_TIME_ 为空时用它回退。 */
    private LocalDateTime snapshotClaimTime;
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

    /** 该任务上存在驳回或自动驳回处理记录；用于把「本人驳回」与「被牵连终止」区分开。 */
    private Boolean rejectedByRecord;
    private LocalDateTime lastReturnAt;
    private String lastReturnVariables;
}
