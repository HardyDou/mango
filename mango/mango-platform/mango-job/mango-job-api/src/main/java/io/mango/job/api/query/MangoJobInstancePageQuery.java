package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Job 执行实例分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Job 执行实例分页查询")
public class MangoJobInstancePageQuery extends PageQuery {

    @Schema(description = "任务 ID")
    private Long jobId;

    @Schema(description = "实例状态")
    private String status;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "触发批次号")
    private String triggerBatchNo;

    @Schema(description = "开始触发时间")
    private LocalDateTime triggerTimeStart;

    @Schema(description = "结束触发时间")
    private LocalDateTime triggerTimeEnd;
}
