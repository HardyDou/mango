package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务工作流申请进度视图。
 */
@Data
@Schema(description = "业务工作流申请进度视图")
public class WorkflowBusinessApplyProgressVO {

    @Schema(description = "申请ID")
    private Long applyId;

    @Schema(description = "申请编号")
    private String applyCode;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "申请标题")
    private String applyTitle;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "流程名称")
    private String processName;

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

    @Schema(description = "当前任务")
    private List<WorkflowBusinessApplyCurrentTaskVO> currentTasks;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
