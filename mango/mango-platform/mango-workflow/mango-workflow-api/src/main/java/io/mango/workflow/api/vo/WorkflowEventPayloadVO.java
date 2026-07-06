package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流标准领域事件载荷。
 */
@Data
@Schema(description = "工作流标准领域事件载荷")
public class WorkflowEventPayloadVO {

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "业务申请ID")
    private String applyId;

    @Schema(description = "流程变量快照")
    private Map<String, Object> variables;

    @Schema(description = "Flowable 流程定义ID")
    private String processDefinitionId;

    @Schema(description = "Mango 流程定义ID")
    private Long definitionId;

    @Schema(description = "流程定义编码")
    private String definitionKey;

    @Schema(description = "流程定义名称")
    private String definitionName;

    @Schema(description = "当前动作任务ID")
    private String taskId;

    @Schema(description = "当前动作任务名称")
    private String taskName;

    @Schema(description = "当前动作任务定义Key")
    private String taskDefinitionKey;

    @Schema(description = "任务处理人")
    private String assignee;

    @Schema(description = "任务处理人ID")
    private String assigneeId;

    @Schema(description = "任务处理人名称")
    private String assigneeName;

    @Schema(description = "完成任务ID")
    private String completedTaskId;

    @Schema(description = "完成任务定义Key")
    private String completedTaskDefinitionKey;

    @Schema(description = "完成任务名称")
    private String completedTaskName;

    @Schema(description = "审批意见或动作备注")
    private String comment;

    @Schema(description = "流程是否已结束")
    private Boolean ended;

    @Schema(description = "结束或拒绝原因")
    private String reason;

    @Schema(description = "申请状态")
    private String applyStatus;

    @Schema(description = "申请状态名称")
    private String applyStatusName;

    @Schema(description = "当前任务名称聚合")
    private String currentTaskNames;

    @Schema(description = "当前任务定义Key聚合")
    private String currentTaskDefinitionKeys;

    @Schema(description = "当前处理人名称聚合")
    private String currentAssigneeNames;

    @Schema(description = "当前任务快照")
    private WorkflowBusinessApplyCurrentTaskVO currentTask;

    @Schema(description = "当前任务快照列表")
    private List<WorkflowBusinessApplyCurrentTaskVO> currentTasks;

    @Schema(description = "任务认领状态")
    private String claimStatus;

    @Schema(description = "候选用户")
    private List<String> candidateUsers;

    @Schema(description = "候选用户组")
    private List<String> candidateGroups;
}
