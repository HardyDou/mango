package io.mango.workflow.core.model;

import lombok.Data;

/**
 * 审批节点动作配置。
 */
@Data
public class WorkflowNodeActionConfig {

    private Boolean enabled;
    private String label;
    private Boolean requireComment;
    private String confirmText;
    private Boolean danger;
    private Integer order;
    private Boolean disabled;
    private String tooltip;
}
