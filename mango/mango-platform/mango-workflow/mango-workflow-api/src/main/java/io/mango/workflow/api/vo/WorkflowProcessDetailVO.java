package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作流流程实例详情视图。
 */
@Data
@Schema(description = "工作流流程实例详情视图")
public class WorkflowProcessDetailVO {

    @Schema(description = "流程实例信息")
    private WorkflowProcessInstanceVO process;

    @Schema(description = "表单编码")
    private String formCode;

    @Schema(description = "表单JSON")
    private String formJson;

    @Schema(description = "流程实例实际运行版本的设计器JSON，用于业务流程图渲染")
    private String designerJson;

    @Schema(description = "当前变量")
    private WorkflowJsonVO variables;

    @Schema(description = "申请/审批页面渲染协议")
    private WorkflowRenderConfigVO renderConfig;

    @Schema(description = "审批记录")
    private List<WorkflowTaskRecordVO> records;
}
