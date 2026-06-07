package io.mango.job.api.command;

import io.mango.job.api.enums.JobTriggerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Mango Job Worker 执行命令。
 */
@Data
@Schema(description = "Mango Job Worker 执行命令")
public class MangoJobWorkerExecuteCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "租户 ID 不能为空")
    @Schema(description = "租户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantId;

    @NotBlank(message = "所属应用不能为空")
    @Schema(description = "所属应用", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "执行服务编码不能为空")
    @Schema(description = "执行服务编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerService;

    @NotBlank(message = "Worker 分组不能为空")
    @Schema(description = "Worker 分组", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerGroup;

    @NotBlank(message = "任务编码不能为空")
    @Schema(description = "任务编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobCode;

    @NotBlank(message = "处理器名称不能为空")
    @Schema(description = "处理器名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String handlerName;

    @NotNull(message = "实例 ID 不能为空")
    @Schema(description = "执行实例 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long instanceId;

    @Schema(description = "操作人 ID")
    private Long operatorId;

    @NotNull(message = "触发类型不能为空")
    @Schema(description = "触发类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private JobTriggerType triggerType;

    @Schema(description = "触发批次号")
    private String triggerBatchNo;

    @NotBlank(message = "链路追踪 ID 不能为空")
    @Schema(description = "链路追踪 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String traceId;

    @Schema(description = "任务参数 JSON")
    private String parameter;
}
