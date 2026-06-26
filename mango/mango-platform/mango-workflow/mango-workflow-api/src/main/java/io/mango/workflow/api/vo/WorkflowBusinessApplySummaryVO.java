package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流我的申请统计视图。
 */
@Data
@Schema(description = "工作流我的申请统计视图")
public class WorkflowBusinessApplySummaryVO {

    @Schema(description = "审核中数量，包含已提交和审批中的申请")
    private Long inReview;

    @Schema(description = "已完成数量，审批通过的申请")
    private Long completed;

    @Schema(description = "已驳回数量，审批驳回的申请")
    private Long rejected;

    @Schema(description = "已撤回数量，申请人已撤回的申请")
    private Long withdrawn;
}
