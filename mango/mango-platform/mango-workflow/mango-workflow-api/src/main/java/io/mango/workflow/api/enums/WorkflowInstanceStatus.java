package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流实例运行状态。
 */
@Getter
@AllArgsConstructor
public enum WorkflowInstanceStatus {

    RUNNING("运行中"),
    COMPLETED("已完成"),
    REJECTED("已驳回"),
    ENDED("已结束");

    private final String label;

    public static WorkflowInstanceStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (WorkflowInstanceStatus status : values()) {
            if (status.name().equalsIgnoreCase(code.trim())) {
                return status;
            }
        }
        return null;
    }

    public static String labelOf(String code, WorkflowInstanceStatus fallback) {
        WorkflowInstanceStatus status = fromCode(code);
        return (status == null ? fallback : status).getLabel();
    }
}
