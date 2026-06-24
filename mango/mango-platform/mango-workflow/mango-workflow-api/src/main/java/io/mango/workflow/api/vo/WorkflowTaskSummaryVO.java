package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流待办统计视图。
 */
@Data
@Schema(description = "工作流待办统计视图")
public class WorkflowTaskSummaryVO {

    @Schema(description = "待审批数量，当前用户已分配待办")
    private Long pendingApproval;

    @Schema(description = "待处理数量，当前用户可认领或候选待办")
    private Long pendingHandle;

    @Schema(description = "待确认数量，当前用户未读抄送")
    private Long pendingConfirm;

    @Schema(description = "已超时数量，当前用户相关待办中超过到期时间的任务")
    private Long overdue;
}
