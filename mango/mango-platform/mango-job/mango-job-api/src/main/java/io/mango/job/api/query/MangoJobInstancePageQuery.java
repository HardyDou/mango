package io.mango.job.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Positive(message = "任务 ID 必须大于0")
    @Schema(description = "任务 ID")
    private Long jobId;

    @Size(max = 32, message = "实例状态不能超过32个字符")
    @Schema(description = "实例状态")
    private String status;

    @Size(max = 32, message = "触发类型不能超过32个字符")
    @Schema(description = "触发类型")
    private String triggerType;

    @Size(max = 128, message = "触发批次号不能超过128个字符")
    @Schema(description = "触发批次号")
    private String triggerBatchNo;

    @PastOrPresent(message = "开始触发时间不能晚于当前时间")
    @Schema(description = "开始触发时间")
    private LocalDateTime triggerTimeStart;

    @PastOrPresent(message = "结束触发时间不能晚于当前时间")
    @Schema(description = "结束触发时间")
    private LocalDateTime triggerTimeEnd;
}
