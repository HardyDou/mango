package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

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

    @Schema(description = "当前变量")
    private Map<String, Object> variables;

    @Schema(description = "申请/审批页面渲染协议")
    private WorkflowRenderConfigVO renderConfig;

    @Schema(description = "审批记录")
    private List<WorkflowTaskRecordVO> records;
}
