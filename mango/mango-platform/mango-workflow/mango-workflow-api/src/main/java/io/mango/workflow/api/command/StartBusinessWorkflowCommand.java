package io.mango.workflow.api.command;

import jakarta.validation.constraints.NotNull;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;


import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;
import java.util.List;

/**
 * 业务流程一体化启动命令。
 */
@Data
@Schema(description = "业务流程一体化启动命令")
public class StartBusinessWorkflowCommand {

    @Schema(description = "Mango流程定义ID")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Long definitionId;

    @Schema(description = "流程定义编码，definitionId 为空时按编码发起最新已发布流程")
    @Size(max = 128, message = "流程定义编码最多128个字符")
    private String definitionKey;

    @Schema(description = "业务类型")
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 128, message = "业务类型最多128个字符")
    private String businessType;

    @Schema(description = "业务主键")
    @NotBlank(message = "业务主键不能为空")
    @Size(max = 128, message = "业务主键最多128个字符")
    private String businessKey;

    @Schema(description = "申请编号")
    @Size(max = 128, message = "申请编号最多128个字符")
    private String applyCode;

    @Schema(description = "申请标题")
    @NotBlank(message = "申请标题不能为空")
    @Size(max = 255, message = "申请标题最多255个字符")
    private String applyTitle;

    @Schema(description = "申请摘要")
    @Size(max = 1000, message = "申请摘要最多1000个字符")
    private String applySummary;

    @Schema(description = "申请审批渲染模式")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private WorkflowApplyRenderMode renderMode;

    @Schema(description = "自定义申请页Key")
    @Size(max = 128, message = "自定义申请页Key最多128个字符")
    private String applyPageKey;

    @Schema(description = "自定义审批页Key")
    @Size(max = 128, message = "自定义审批页Key最多128个字符")
    private String approvePageKey;

    @Schema(description = "表单Key")
    @Size(max = 128, message = "表单Key最多128个字符")
    private String formKey;

    @Schema(description = "表单版本")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Integer formVersion;

    @Schema(description = "动态表单JSON快照")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private String formJsonSnapshot;

    @Schema(description = "动态表单数据快照")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private String formDataSnapshot;

    @Schema(description = "业务快照引用")
    @Size(max = 255, message = "业务快照引用最多255个字符")
    private String snapshotRef;

    @Schema(description = "业务快照摘要")
    @Size(max = 128, message = "业务快照摘要最多128个字符")
    private String snapshotDigest;

    @Schema(description = "流程变量")
    @NotNull(groups = WorkflowOptionalValidation.class)
    @jakarta.validation.Valid
    private WorkflowJsonRequest variables;

    @Schema(description = "扩展配置")
    @NotNull(groups = WorkflowOptionalValidation.class)
    @jakarta.validation.Valid
    private WorkflowJsonRequest extension;

    @Schema(description = "发起人自选审批人，key 为节点ID或节点定义Key，value 为用户ID/用户名数组")
    @NotNull(groups = WorkflowOptionalValidation.class)
    @jakarta.validation.Valid
    private WorkflowJsonRequest selectedAssignees;

    @Schema(description = "业务声明的只读参与用户ID完整集合")
    @Size(max = 200, message = "业务参与用户最多200个")
    private List<@NotNull(message = "业务参与用户ID不能为空") Long> participantUserIds;
}
