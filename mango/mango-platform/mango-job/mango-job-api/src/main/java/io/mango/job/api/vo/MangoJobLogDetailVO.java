package io.mango.job.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Job 执行日志详情返回对象。
 */
@Data
@Schema(description = "Job 执行日志详情返回对象")
public class MangoJobLogDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志索引 ID")
    private Long id;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "任务 ID")
    private Long jobId;

    @Schema(description = "任务编码")
    private String jobCode;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "实例 ID")
    private Long instanceId;

    @Schema(description = "Mango 实例状态")
    private String instanceStatus;

    @Schema(description = "触发批次号")
    private String triggerBatchNo;

    @Schema(description = "引擎类型")
    private String engineType;

    @Schema(description = "引擎实例标识")
    private String engineInstanceId;

    @Schema(description = "日志位置或拉取游标")
    private String logLocation;

    @Schema(description = "读取偏移量")
    private Long readOffset;

    @Schema(description = "日志来源")
    private String logSource;

    @Schema(description = "原生日志是否可用")
    private Boolean nativeLogAvailable;

    @Schema(description = "日志拉取状态")
    private String logFetchStatus;

    @Schema(description = "调度引擎原生执行日志内容")
    private String nativeLogContent;

    @Schema(description = "兼容字段，等同 nativeLogContent")
    private String content;

    @Schema(description = "执行结果兜底内容")
    private String engineResult;

    @Schema(description = "错误摘要")
    private String errorSummary;

    @Schema(description = "最近拉取时间")
    private LocalDateTime lastFetchedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
