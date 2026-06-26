package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作流我的任务统计视图。
 */
@Data
@Schema(description = "工作流我的任务统计视图")
public class WorkflowMyTaskSummaryVO {

    @Schema(description = "任务总数，待完成、进行中、已完成和已逾期数量合计")
    private Long total;

    @Schema(description = "待完成数量，当前用户可认领或候选处理的运行任务")
    private Long pending;

    @Schema(description = "进行中数量，当前用户已分配待处理的运行任务")
    private Long processing;

    @Schema(description = "已完成数量，当前用户已处理完成的历史任务")
    private Long completed;

    @Schema(description = "已逾期数量，当前用户相关运行任务中超过到期时间的任务")
    private Long overdue;
}
