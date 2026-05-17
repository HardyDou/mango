package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批驳回处理策略。
 */
@Getter
@AllArgsConstructor
public enum WorkflowRejectStrategy {

    END_PROCESS("直接结束流程"),
    BACK_TO_START("驳回到发起人");

    private final String label;

    public static WorkflowRejectStrategy fromCode(String code, WorkflowRejectStrategy fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        String normalized = code.trim();
        for (WorkflowRejectStrategy strategy : values()) {
            if (strategy.name().equalsIgnoreCase(normalized)) {
                return strategy;
            }
        }
        return switch (normalized.toUpperCase()) {
            case "TO_END", "END" -> END_PROCESS;
            case "TO_START", "START" -> BACK_TO_START;
            default -> fallback;
        };
    }
}
