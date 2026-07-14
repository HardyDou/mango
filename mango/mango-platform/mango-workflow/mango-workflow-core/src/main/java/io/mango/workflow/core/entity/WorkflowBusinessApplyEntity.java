package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 业务工作流申请记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_business_apply")
public class WorkflowBusinessApplyEntity extends WorkflowBaseEntity {

    private String applyCode;
    private String businessType;
    private String businessKey;
    private String applyTitle;
    private String applySummary;
    private Long applicantId;
    private String applicantName;
    private Long applicantDeptId;
    private String applicantDeptName;
    private Long processDefinitionId;
    private String processDefinitionKey;
    private String engineProcessDefinitionId;
    private String processInstanceId;
    private String processName;
    private String applyStatus;
    private String currentTaskNames;
    private String currentTaskDefinitionKeys;
    private String currentAssigneeNames;
    private String renderMode;
    private String applyPageKey;
    private String approvePageKey;
    private String formKey;
    private Integer formVersion;
    private String formJsonSnapshot;
    private String formDataSnapshot;
    private String snapshotRef;
    private String snapshotDigest;
    private String variablesJson;
    private String extensionJson;
    private Long reapplyFromApplyId;
    private Boolean latestFlag;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
