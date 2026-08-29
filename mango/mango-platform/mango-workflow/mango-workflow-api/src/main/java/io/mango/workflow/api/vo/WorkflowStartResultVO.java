package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 业务流程启动结果视图。
 */
@Data
@Schema(description = "业务流程启动结果视图")
public class WorkflowStartResultVO {

    @Schema(description = "业务申请ID")
    private Long applyId;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

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

    @Schema(description = "Flowable 原始办理人 key")
    private String assigneeName;

    @Schema(description = "处理人显示名；昵称优先、用户名兜底，无法解析时为空")
    private String assigneeDisplayName;

    @Schema(description = "认领状态")
    private WorkflowTaskClaimStatus claimStatus;

    @Schema(description = "候选用户")
    private List<String> candidateUsers;

    @Schema(description = "候选组")
    private List<String> candidateGroups;

    @Schema(description = "当前任务")
    private List<WorkflowBusinessApplyCurrentTaskVO> currentTasks;
}
