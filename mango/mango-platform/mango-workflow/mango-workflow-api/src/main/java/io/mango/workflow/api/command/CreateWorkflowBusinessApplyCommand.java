package io.mango.workflow.api.command;

import io.mango.workflow.api.query.WorkflowApplyAttributeQuery;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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

    @Schema(description = "稳定业务聚合键，跨提交保持不变；用于节点亲和跨流程实例复用办理人")
    @Size(max = 128, message = "亲和键最多128个字符")
    private String affinityKey;

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

    @Schema(description = "申请人所属组织ID；业务模块有明确组织事实时传入")
    @Positive(message = "申请人所属组织ID必须为正数")
    private Long applicantDeptId;

    @Schema(description = "Mango流程定义ID")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Long processDefinitionId;

    @Schema(description = "流程定义编码")
    @Size(max = 128, message = "流程定义编码最多128个字符")
    private String processDefinitionKey;

    @Schema(description = "渲染模式")
    @NotNull(groups = WorkflowOptionalValidation.class)
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

    @Schema(description = "业务快照摘要或校验值")
    @Size(max = 128, message = "业务快照摘要最多128个字符")
    private String snapshotDigest;

    @Schema(description = "重新申请来源ID")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Long reapplyFromApplyId;

    @Schema(description = "流程变量")
    @NotNull(groups = WorkflowOptionalValidation.class)
    @jakarta.validation.Valid
    private WorkflowJsonRequest variables;

    @Schema(description = "扩展配置")
    @NotNull(groups = WorkflowOptionalValidation.class)
    @jakarta.validation.Valid
    private WorkflowJsonRequest extension;

    @Schema(description = "可筛属性：业务自定义键值，供任务列表精确过滤和消息内容使用；最多20个属性，"
            + "属性名以字母开头且只允许字母、数字、下划线、点和短横线，"
            + "属性值不超过255字符，单属性最多50个值")
    @Size(max = 20, message = "可筛属性数量不能超过20")
    @Valid
    private List<WorkflowApplyAttributeQuery> filterAttributes;
}
