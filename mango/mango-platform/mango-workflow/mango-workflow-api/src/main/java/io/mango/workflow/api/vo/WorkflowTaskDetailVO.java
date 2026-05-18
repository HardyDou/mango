package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流任务详情视图。
 */
@Data
@Schema(description = "工作流任务详情视图")
public class WorkflowTaskDetailVO {

    @Schema(description = "任务信息")
    private WorkflowTaskVO task;

    @Schema(description = "流程实例信息")
    private WorkflowProcessInstanceVO process;

    @Schema(description = "表单编码")
    private String formCode;

    @Schema(description = "表单JSON")
    private String formJson;

    @Schema(description = "当前变量")
    private Map<String, Object> variables;

    @Schema(description = "当前节点表单字段权限，key 为字段标识，value 为 HIDDEN/READONLY/EDITABLE")
    private Map<String, String> formPermissions;

    @Schema(description = "申请/审批页面渲染协议")
    private WorkflowRenderConfigVO renderConfig;

    @Schema(description = "审批记录")
    private List<WorkflowTaskRecordVO> records;
}
