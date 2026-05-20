package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流节点审批动作配置。
 */
@Data
@Schema(description = "工作流节点审批动作配置")
public class WorkflowNodeActionConfigVO {

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "按钮文案")
    private String label;

    @Schema(description = "是否必填审批意见")
    private Boolean requireComment;

    @Schema(description = "确认提示文案")
    private String confirmText;

    @Schema(description = "是否危险动作")
    private Boolean danger;

    @Schema(description = "排序值")
    private Integer order;

    @Schema(description = "是否禁用")
    private Boolean disabled;

    @Schema(description = "禁用或提示说明")
    private String tooltip;
}
