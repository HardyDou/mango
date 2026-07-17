package io.mango.job.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步 Job 执行实例命令。
 */
@Data
@Schema(description = "同步 Job 执行实例命令")
public class SyncMangoJobInstanceCommand {

    @Positive(message = "任务 ID 必须大于0")
    @Schema(description = "任务 ID；为空时同步当前租户最近的调度任务")
    private Long jobId;

    @PastOrPresent(message = "开始触发时间不能晚于当前时间")
    @Schema(description = "开始触发时间")
    private LocalDateTime triggerTimeStart;

    @PastOrPresent(message = "结束触发时间不能晚于当前时间")
    @Schema(description = "结束触发时间")
    private LocalDateTime triggerTimeEnd;

    @Positive(message = "单次同步上限必须大于0")
    @Schema(description = "单次同步上限")
    private Long size;
}
