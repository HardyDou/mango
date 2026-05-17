package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务申请状态。
 */
@Getter
@AllArgsConstructor
public enum WorkflowApplyStatus {

    DRAFT("草稿"),
    SUBMITTED("已提交"),
    IN_APPROVAL("审批中"),
    APPROVED("已通过"),
    REJECTED("已驳回"),
    WITHDRAWN("已撤回"),
    CANCELED("已取消"),
    TERMINATED("已终止");

    private final String label;

    public static WorkflowApplyStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (WorkflowApplyStatus status : values()) {
            if (status.name().equalsIgnoreCase(code.trim())) {
                return status;
            }
        }
        return null;
    }
}
