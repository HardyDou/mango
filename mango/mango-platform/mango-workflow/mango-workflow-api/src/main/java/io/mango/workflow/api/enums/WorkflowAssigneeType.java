package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批人来源类型。
 */
@Getter
@AllArgsConstructor
public enum WorkflowAssigneeType {

    SPECIFIED_USER("指定成员"),
    SPECIFIED_ROLE("指定角色"),
    SPECIFIED_POST("指定岗位"),
    SPECIFIED_ORG("指定组织"),
    ORG_LEADER("组织负责人"),
    INITIATOR("发起人自己"),
    INITIATOR_SELECT("发起人自选"),
    FORM_USER("表单人员"),
    EXPRESSION("流程表达式");

    private final String label;

    public static WorkflowAssigneeType fromCode(String code, WorkflowAssigneeType fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        String normalized = code.trim();
        for (WorkflowAssigneeType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return switch (normalized.toUpperCase()) {
            case "USER", "MEMBER", "ASSIGNEE" -> SPECIFIED_USER;
            case "ROLE" -> SPECIFIED_ROLE;
            case "POST", "ORG_POST" -> SPECIFIED_POST;
            case "ORG", "DEPT", "DEPARTMENT" -> SPECIFIED_ORG;
            case "DEPT_LEADER", "APPLICANT_LEADER", "ORG_MANAGER" -> ORG_LEADER;
            case "APPLICANT", "STARTER", "SELF" -> INITIATOR;
            case "STARTER_SELECT", "PROMOTER_SELECT" -> INITIATOR_SELECT;
            case "FORM", "FORM_USER_FIELD" -> FORM_USER;
            default -> fallback;
        };
    }
}
