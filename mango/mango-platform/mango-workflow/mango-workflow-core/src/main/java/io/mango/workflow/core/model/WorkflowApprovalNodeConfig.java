package io.mango.workflow.core.model;

import io.mango.workflow.api.enums.WorkflowApprovalMode;
import io.mango.workflow.api.enums.WorkflowAssigneeType;
import io.mango.workflow.api.enums.WorkflowEmptyAssigneeStrategy;
import io.mango.workflow.api.enums.WorkflowFormPermission;
import io.mango.workflow.api.enums.WorkflowRejectStrategy;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批节点结构化配置。
 */
@Data
public class WorkflowApprovalNodeConfig {

    private WorkflowAssigneeType assigneeType = WorkflowAssigneeType.SPECIFIED_USER;
    private List<String> assigneeIds = new ArrayList<>();
    private List<String> roleIds = new ArrayList<>();
    private List<String> postIds = new ArrayList<>();
    private List<String> orgIds = new ArrayList<>();
    private String formUserField;
    private String formUserFieldType = "USER";
    private String expression;
    private String expressionName;
    private WorkflowApprovalMode approvalMode = WorkflowApprovalMode.COUNTERSIGN;
    private WorkflowEmptyAssigneeStrategy emptyAssigneeStrategy = WorkflowEmptyAssigneeStrategy.TO_ADMIN;
    private List<String> emptyAssigneeUserIds = new ArrayList<>();
    private WorkflowRejectStrategy rejectStrategy = WorkflowRejectStrategy.END_PROCESS;
    private Map<String, WorkflowFormPermission> formPermissions = new LinkedHashMap<>();
    private WorkflowEventNotifyConfig eventNotify = new WorkflowEventNotifyConfig();
    private boolean initiatorSelectMultiple;
    private boolean orgLeaderUseInitiatorOrg = true;

    public boolean hasEventNotify() {
        return eventNotify != null && eventNotify.enabled();
    }
}
