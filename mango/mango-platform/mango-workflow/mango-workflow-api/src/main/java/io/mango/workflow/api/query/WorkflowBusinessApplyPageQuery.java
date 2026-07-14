package io.mango.workflow.api.query;

import jakarta.validation.constraints.NotNull;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;


import io.mango.common.po.PageQuery;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务工作流申请分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务工作流申请分页查询")
public class WorkflowBusinessApplyPageQuery extends PageQuery {

    @Schema(description = "业务类型")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private String businessType;

    @Schema(description = "业务主键")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private String businessKey;

    @Schema(description = "关键字，支持申请编号、标题、摘要")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private String keyword;

    @Schema(description = "申请状态")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private List<WorkflowApplyStatus> statuses;

    @Schema(description = "是否只查最新申请")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Boolean latestOnly;

    @Schema(description = "申请人ID")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Long applicantId;

    @Schema(description = "当前节点定义Key")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private List<String> currentTaskDefinitionKeys;

    @Schema(description = "当前处理人ID")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private List<Long> currentAssigneeIds;

    @Schema(description = "申请开始时间")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private LocalDateTime startedAtBegin;

    @Schema(description = "申请结束时间")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private LocalDateTime startedAtEnd;
}
