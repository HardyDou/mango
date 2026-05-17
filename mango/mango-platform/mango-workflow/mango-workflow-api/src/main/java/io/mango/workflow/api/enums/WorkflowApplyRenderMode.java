package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务申请页面渲染模式。
 */
@Getter
@AllArgsConstructor
public enum WorkflowApplyRenderMode {

    DYNAMIC_FORM("动态表单"),
    CUSTOM_PAGE("自定义页面");

    private final String label;

    public static WorkflowApplyRenderMode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (WorkflowApplyRenderMode mode : values()) {
            if (mode.name().equalsIgnoreCase(code.trim())) {
                return mode;
            }
        }
        return null;
    }
}
