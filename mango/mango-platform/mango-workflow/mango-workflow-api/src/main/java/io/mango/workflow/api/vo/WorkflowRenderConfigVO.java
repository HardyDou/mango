package io.mango.workflow.api.vo;

import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 工作流申请/审批页面渲染协议。
 */
@Data
@Schema(description = "工作流申请/审批页面渲染协议")
public class WorkflowRenderConfigVO {

    @Schema(description = "渲染模式：DYNAMIC_FORM/CUSTOM_PAGE")
    private WorkflowApplyRenderMode renderMode;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "申请ID")
    private Long applyId;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "自定义申请页Key")
    private String applyPageKey;

    @Schema(description = "自定义审批页Key")
    private String approvePageKey;

    @Schema(description = "表单编码")
    private String formKey;

    @Schema(description = "表单版本")
    private Integer formVersion;

    @Schema(description = "业务快照引用")
    private String snapshotRef;

    @Schema(description = "当前节点定义Key")
    private String taskDefinitionKey;

    @Schema(description = "当前节点扩展属性")
    private Map<String, Object> nodeExtension;

    @Schema(description = "字段权限，key 为字段标识，value 为 HIDDEN/READONLY/EDITABLE")
    private Map<String, String> formPermissions;

    @Schema(description = "业务页面权限配置")
    private Map<String, Object> businessPermissions;

    @Schema(description = "当前节点审批动作配置，key 为 complete/reject/save/transfer/addSign")
    private Map<String, WorkflowNodeActionConfigVO> nodeActions;
}
