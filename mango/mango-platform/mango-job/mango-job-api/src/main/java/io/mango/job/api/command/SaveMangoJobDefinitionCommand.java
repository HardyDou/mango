package io.mango.job.api.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存 Job 任务定义命令。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "保存 Job 任务定义命令")
public class SaveMangoJobDefinitionCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务 ID。新增时为空，修改时必填")
    private Long id;

    @NotBlank(message = "所属应用不能为空")
    @Size(max = 128, message = "所属应用不能超过128个字符")
    @Schema(description = "所属逻辑应用", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "任务编码不能为空")
    @Size(max = 128, message = "任务编码不能超过128个字符")
    @Schema(description = "任务编码，租户和应用内唯一", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobCode;

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 128, message = "任务名称不能超过128个字符")
    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobName;

    @NotBlank(message = "任务类型不能为空")
    @Size(max = 32, message = "任务类型不能超过32个字符")
    @Schema(description = "任务类型：BUILTIN", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobType;

    @NotBlank(message = "调度类型不能为空")
    @Size(max = 32, message = "调度类型不能超过32个字符")
    @Schema(description = "调度类型：CRON、FIXED_RATE、ONE_TIME、MANUAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scheduleType;

    @Size(max = 256, message = "调度表达式不能超过256个字符")
    @Schema(description = "调度表达式。CRON、FIXED_RATE、ONE_TIME 类型必填")
    private String scheduleExpression;

    @Size(max = 256, message = "处理器名称不能超过256个字符")
    @Schema(description = "处理器名称。BUILTIN 类型必填")
    private String handlerName;

    @Schema(description = "参数表单 schema JSON")
    private String paramSchema;

    @Schema(description = "默认参数 JSON")
    private String paramValue;

    @Size(max = 64, message = "错过触发策略不能超过64个字符")
    @Schema(description = "错过触发策略")
    private String misfireStrategy;

    @Size(max = 64, message = "并发策略不能超过64个字符")
    @Schema(description = "并发策略")
    private String concurrencyPolicy;

    @Min(value = 1, message = "执行超时必须大于0")
    @Schema(description = "执行超时秒数")
    private Integer timeoutSeconds;

    @Schema(description = "重试策略 JSON")
    private String retryPolicy;

    @NotNull(message = "引擎类型不能为空")
    @Size(max = 32, message = "引擎类型不能超过32个字符")
    @Schema(description = "引擎类型：POWERJOB", requiredMode = Schema.RequiredMode.REQUIRED)
    private String engineType;
}
