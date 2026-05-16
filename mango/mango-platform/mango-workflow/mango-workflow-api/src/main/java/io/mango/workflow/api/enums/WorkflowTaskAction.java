package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流任务处理动作。
 */
@Getter
@AllArgsConstructor
public enum WorkflowTaskAction {

    START("发起"),
    COMPLETE("通过"),
    REJECT("驳回"),
    AUTO_COMPLETE("自动通过"),
    AUTO_REJECT("自动驳回"),
    AUTO_END("自动结束"),
    EVENT_NOTIFY("事件通知");

    private final String label;

    public static WorkflowTaskAction fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (WorkflowTaskAction action : values()) {
            if (action.name().equalsIgnoreCase(code.trim())) {
                return action;
            }
        }
        return null;
    }

    public static String labelOf(String code) {
        WorkflowTaskAction action = fromCode(code);
        return action == null ? null : action.getLabel();
    }
}
