package io.mango.workflow.api.query;

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
    private String businessType;

    @Schema(description = "业务主键")
    private String businessKey;

    @Schema(description = "关键字，支持申请编号、标题、摘要")
    private String keyword;

    @Schema(description = "申请状态")
    private List<WorkflowApplyStatus> statuses;

    @Schema(description = "是否只查最新申请")
    private Boolean latestOnly;

    @Schema(description = "申请人ID")
    private Long applicantId;

    @Schema(description = "当前节点定义Key")
    private List<String> currentTaskDefinitionKeys;

    @Schema(description = "当前处理人ID")
    private List<Long> currentAssigneeIds;

    @Schema(description = "申请开始时间")
    private LocalDateTime startedAtBegin;

    @Schema(description = "申请结束时间")
    private LocalDateTime startedAtEnd;
}
