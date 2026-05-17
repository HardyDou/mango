package io.mango.workflow.api.command;

import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 创建业务工作流申请命令。
 */
@Data
@Schema(description = "创建业务工作流申请命令")
public class CreateWorkflowBusinessApplyCommand {

    @Schema(description = "业务类型")
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 128, message = "业务类型最多128个字符")
    private String businessType;

    @Schema(description = "业务主键")
    @NotBlank(message = "业务主键不能为空")
    @Size(max = 128, message = "业务主键最多128个字符")
    private String businessKey;

    @Schema(description = "申请编号，可为空；为空时后端生成")
    @Size(max = 128, message = "申请编号最多128个字符")
    private String applyCode;

    @Schema(description = "申请标题")
    @NotBlank(message = "申请标题不能为空")
    @Size(max = 255, message = "申请标题最多255个字符")
    private String applyTitle;

    @Schema(description = "申请摘要")
    @Size(max = 1000, message = "申请摘要最多1000个字符")
    private String applySummary;

    @Schema(description = "Mango流程定义ID")
    private Long processDefinitionId;

    @Schema(description = "流程定义编码")
    @Size(max = 128, message = "流程定义编码最多128个字符")
    private String processDefinitionKey;

    @Schema(description = "渲染模式")
    private WorkflowApplyRenderMode renderMode = WorkflowApplyRenderMode.DYNAMIC_FORM;

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
    private Integer formVersion;

    @Schema(description = "动态表单JSON快照")
    private String formJsonSnapshot;

    @Schema(description = "动态表单数据快照")
    private String formDataSnapshot;

    @Schema(description = "业务快照引用")
    @Size(max = 255, message = "业务快照引用最多255个字符")
    private String snapshotRef;

    @Schema(description = "业务快照摘要或校验值")
    @Size(max = 128, message = "业务快照摘要最多128个字符")
    private String snapshotDigest;

    @Schema(description = "重新申请来源ID")
    private Long reapplyFromApplyId;

    @Schema(description = "流程变量")
    private Map<String, Object> variables;

    @Schema(description = "扩展配置")
    private Map<String, Object> extension;
}
