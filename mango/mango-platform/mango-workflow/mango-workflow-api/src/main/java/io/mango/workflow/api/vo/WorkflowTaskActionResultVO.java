package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作流任务动作结果视图。
 */
@Data
@Schema(description = "工作流任务动作结果视图")
public class WorkflowTaskActionResultVO {

    @Schema(description = "任务动作")
    private WorkflowTaskAction actionResult;

    @Schema(description = "已处理任务ID")
    private String previousTaskId;

    @Schema(description = "已处理任务定义Key")
    private String previousTaskDefinitionKey;

    @Schema(description = "已处理任务名称")
    private String previousTaskName;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "业务申请ID")
    private Long applyId;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "流程状态")
    private WorkflowApplyStatus processStatus;

    @Schema(description = "流程状态名称")
    private String processStatusName;

    @Schema(description = "当前任务ID")
    private String currentTaskId;

    @Schema(description = "当前任务名称")
    private String currentTaskName;

    @Schema(description = "当前任务定义Key")
    private String taskDefinitionKey;

    @Schema(description = "处理人ID")
    private Long assigneeId;

    @Schema(description = "处理人名称")
    private String assigneeName;

    @Schema(description = "认领状态")
    private WorkflowTaskClaimStatus claimStatus;

    @Schema(description = "候选用户")
    private List<String> candidateUsers;

    @Schema(description = "候选组")
    private List<String> candidateGroups;

    @Schema(description = "当前任务")
    private WorkflowBusinessApplyCurrentTaskVO currentTask;

    @Schema(description = "后续任务")
    private List<WorkflowBusinessApplyCurrentTaskVO> nextTasks;

    @Schema(description = "流程是否已结束")
    private Boolean ended;

    @Schema(description = "流程是否已取消")
    private Boolean cancelled;

    @Schema(description = "流程是否已驳回")
    private Boolean rejected;
}
