package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点表单字段权限。
 */
@Getter
@AllArgsConstructor
public enum WorkflowFormPermission {

    HIDDEN("隐藏"),
    READONLY("只读"),
    EDITABLE("可编辑");

    private final String label;

    public static WorkflowFormPermission fromCode(String code, WorkflowFormPermission fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        String normalized = code.trim();
        for (WorkflowFormPermission permission : values()) {
            if (permission.name().equalsIgnoreCase(normalized)) {
                return permission;
            }
        }
        return switch (normalized.toUpperCase()) {
            case "H" -> HIDDEN;
            case "R" -> READONLY;
            case "E" -> EDITABLE;
            default -> fallback;
        };
    }
}
