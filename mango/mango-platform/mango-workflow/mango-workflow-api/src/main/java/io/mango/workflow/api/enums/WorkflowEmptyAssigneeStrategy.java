package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批人为空时处理策略。
 */
@Getter
@AllArgsConstructor
public enum WorkflowEmptyAssigneeStrategy {

    AUTO_PASS("自动通过"),
    AUTO_REJECT("自动驳回"),
    AUTO_END("自动结束"),
    TO_ADMIN("转交管理员"),
    TO_USER("转交指定成员");

    private final String label;

    public static WorkflowEmptyAssigneeStrategy fromCode(String code, WorkflowEmptyAssigneeStrategy fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        String normalized = code.trim();
        for (WorkflowEmptyAssigneeStrategy strategy : values()) {
            if (strategy.name().equalsIgnoreCase(normalized)) {
                return strategy;
            }
        }
        return switch (normalized.toUpperCase()) {
            case "TO_PASS", "PASS" -> AUTO_PASS;
            case "TO_REFUSE", "REFUSE", "REJECT" -> AUTO_REJECT;
            case "TO_END", "END" -> AUTO_END;
            case "ADMIN", "TRANSFER_ADMIN" -> TO_ADMIN;
            case "USER", "TRANSFER_USER" -> TO_USER;
            default -> fallback;
        };
    }
}
