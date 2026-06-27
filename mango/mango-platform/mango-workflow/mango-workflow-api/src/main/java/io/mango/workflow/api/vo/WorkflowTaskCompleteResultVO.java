package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Workflow task completion result after runtime advancement.
 */
@Data
@Schema(description = "审批任务完成后流程推进结果")
public class WorkflowTaskCompleteResultVO {

    @Schema(description = "已完成任务ID")
    private String completedTaskId;

    @Schema(description = "已完成任务定义Key")
    private String completedTaskDefinitionKey;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "流程是否已结束")
    private Boolean ended;

    @Schema(description = "业务申请ID")
    private Long applyId;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "申请状态")
    private WorkflowApplyStatus applyStatus;

    @Schema(description = "申请状态名称")
    private String applyStatusName;

    @Schema(description = "当前节点名称")
    private String currentTaskNames;

    @Schema(description = "当前节点定义Key")
    private String currentTaskDefinitionKeys;

    @Schema(description = "当前处理人名称")
    private String currentAssigneeNames;

    @Schema(description = "推进后的当前任务")
    private List<WorkflowBusinessApplyCurrentTaskVO> currentTasks;
}
