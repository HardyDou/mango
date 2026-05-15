package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 多人审批方式。
 */
@Getter
@AllArgsConstructor
public enum WorkflowApprovalMode {

    COUNTERSIGN("会签"),
    OR_SIGN("或签"),
    SEQUENTIAL("依次审批");

    private final String label;

    public static WorkflowApprovalMode fromCode(String code, WorkflowApprovalMode fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        String normalized = code.trim();
        for (WorkflowApprovalMode mode : values()) {
            if (mode.name().equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        return switch (normalized.toUpperCase()) {
            case "ALL", "AND", "MULTI", "COUNTER_SIGN" -> COUNTERSIGN;
            case "ANY", "OR", "ONE", "OR_SIGN" -> OR_SIGN;
            case "ORDER", "SERIAL", "SEQUENCE" -> SEQUENTIAL;
            default -> fallback;
        };
    }
}
