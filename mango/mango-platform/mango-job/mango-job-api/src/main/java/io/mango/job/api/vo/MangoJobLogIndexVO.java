package io.mango.job.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Job 日志索引返回对象。
 */
@Data
@Schema(description = "Job 日志索引返回对象")
public class MangoJobLogIndexVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志索引 ID")
    private Long id;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "任务 ID")
    private Long jobId;

    @Schema(description = "实例 ID")
    private Long instanceId;

    @Schema(description = "引擎类型")
    private String engineType;

    @Schema(description = "引擎实例标识")
    private String engineInstanceId;

    @Schema(description = "日志位置或拉取游标")
    private String logLocation;

    @Schema(description = "读取偏移量")
    private Long readOffset;

    @Schema(description = "错误摘要")
    private String errorSummary;

    @Schema(description = "最近拉取时间")
    private LocalDateTime lastFetchedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
