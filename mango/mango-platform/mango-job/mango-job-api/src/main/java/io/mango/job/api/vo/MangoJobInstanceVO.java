package io.mango.job.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Job 执行实例返回对象。
 */
@Data
@Schema(description = "Job 执行实例返回对象")
public class MangoJobInstanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实例 ID")
    private Long id;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "任务 ID")
    private Long jobId;

    @Schema(description = "任务编码")
    private String jobCode;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务名称快照")
    private String jobNameSnapshot;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "触发人 ID")
    private Long triggerUserId;

    @Schema(description = "触发时间")
    private LocalDateTime triggerTime;

    @Schema(description = "计划触发时间")
    private LocalDateTime scheduledFireTime;

    @Schema(description = "实际触发时间")
    private LocalDateTime actualFireTime;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "实例状态")
    private String status;

    @Schema(description = "执行耗时毫秒")
    private Long durationMillis;

    @Schema(description = "执行尝试次数")
    private Integer attemptCount;

    @Schema(description = "结果摘要")
    private String resultSummary;

    @Schema(description = "Worker 地址")
    private String workerAddress;

    @Schema(description = "引擎类型")
    private String engineType;

    @Schema(description = "引擎实例标识")
    private String engineInstanceId;

    @Schema(description = "错误摘要")
    private String errorSummary;

    @Schema(description = "链路追踪 ID")
    private String traceId;

    @Schema(description = "触发批次号")
    private String triggerBatchNo;
}
