package io.mango.job.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步 Job 执行实例命令。
 */
@Data
@Schema(description = "同步 Job 执行实例命令")
public class SyncMangoJobInstanceCommand {

    @Schema(description = "任务 ID；为空时同步当前租户最近的调度任务")
    private Long jobId;

    @Schema(description = "开始触发时间")
    private LocalDateTime triggerTimeStart;

    @Schema(description = "结束触发时间")
    private LocalDateTime triggerTimeEnd;

    @Schema(description = "单次同步上限")
    private Long size;
}
