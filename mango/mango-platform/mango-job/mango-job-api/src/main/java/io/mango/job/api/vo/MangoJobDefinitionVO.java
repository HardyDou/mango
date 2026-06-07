package io.mango.job.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Job 任务定义返回对象。
 */
@Data
@Schema(description = "Job 任务定义返回对象")
public class MangoJobDefinitionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务 ID")
    private Long id;

    @Schema(description = "租户 ID")
    private String tenantId;

    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Schema(description = "执行服务编码")
    private String ownerService;

    @Schema(description = "Worker 分组")
    private String workerGroup;

    @Schema(description = "任务编码")
    private String jobCode;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务类型")
    private String jobType;

    @Schema(description = "调度类型")
    private String scheduleType;

    @Schema(description = "调度表达式")
    private String scheduleExpression;

    @Schema(description = "处理器名称")
    private String handlerName;

    @Schema(description = "参数表单 schema JSON")
    private String paramSchema;

    @Schema(description = "默认参数 JSON")
    private String paramValue;

    @Schema(description = "错过触发策略")
    private String misfireStrategy;

    @Schema(description = "并发策略")
    private String concurrencyPolicy;

    @Schema(description = "执行超时秒数")
    private Integer timeoutSeconds;

    @Schema(description = "重试策略 JSON")
    private String retryPolicy;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "引擎类型")
    private String engineType;

    @Schema(description = "引擎应用标识")
    private String engineAppId;

    @Schema(description = "引擎任务标识")
    private String engineJobId;

    @Schema(description = "引擎同步状态")
    private String syncStatus;

    @Schema(description = "同步错误摘要")
    private String syncError;

    @Schema(description = "创建人 ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新人 ID")
    private Long updatedBy;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
