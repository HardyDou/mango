package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 业务工作流申请视图。
 */
@Data
@Schema(description = "业务工作流申请视图")
public class WorkflowBusinessApplyVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "申请编号")
    private String applyCode;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "申请标题")
    private String applyTitle;

    @Schema(description = "申请摘要")
    private String applySummary;

    @Schema(description = "申请人ID")
    private Long applicantId;

    @Schema(description = "申请人名称")
    private String applicantName;

    @Schema(description = "流程定义ID")
    private Long processDefinitionId;

    @Schema(description = "流程定义编码")
    private String processDefinitionKey;

    @Schema(description = "Flowable流程定义ID")
    private String engineProcessDefinitionId;

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

    @Schema(description = "渲染模式")
    private WorkflowApplyRenderMode renderMode;

    @Schema(description = "自定义申请页Key")
    private String applyPageKey;

    @Schema(description = "自定义审批页Key")
    private String approvePageKey;

    @Schema(description = "表单Key")
    private String formKey;

    @Schema(description = "表单版本")
    private Integer formVersion;

    @Schema(description = "业务快照引用")
    private String snapshotRef;

    @Schema(description = "重新申请来源ID")
    private Long reapplyFromApplyId;

    @Schema(description = "是否最新申请")
    private Boolean latestFlag;

    @Schema(description = "流程变量")
    private Map<String, Object> variables;

    @Schema(description = "扩展配置")
    private Map<String, Object> extension;

    @Schema(description = "当前任务")
    private List<WorkflowBusinessApplyCurrentTaskVO> currentTasks;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
