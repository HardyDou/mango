package io.mango.job.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Worker 注册时声明的 Job 处理器能力。
 */
@Data
@Schema(description = "Worker Job 处理器能力")
public class MangoJobHandlerCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 128, message = "所属应用不能超过128个字符")
    @Schema(description = "所属逻辑应用。为空时继承 Worker 所属应用")
    private String appCode;

    @Size(max = 128, message = "执行服务编码不能超过128个字符")
    @Schema(description = "执行服务编码。为空时继承 Worker 执行服务")
    private String serviceCode;

    @Size(max = 128, message = "Worker 分组不能超过128个字符")
    @Schema(description = "Worker 分组。为空时继承 Worker 分组")
    private String workerGroup;

    @NotBlank(message = "处理器名称不能为空")
    @Size(max = 128, message = "处理器名称不能超过128个字符")
    @Schema(description = "处理器名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String handlerName;

    @Size(max = 256, message = "支持的任务编码不能超过256项")
    @Schema(description = "支持的任务编码。为空表示不限制 jobCode")
    private Set<@NotBlank(message = "任务编码不能为空")
            @Size(max = 128, message = "任务编码不能超过128个字符") String> supportedJobCodes = new LinkedHashSet<>();

    @Size(max = 64, message = "处理器类型不能超过64个字符")
    @Schema(description = "处理器类型")
    private String jobType;

    @Size(max = 16384, message = "参数表单 schema 不能超过16384个字符")
    @Schema(description = "参数表单 schema JSON")
    private String paramSchema;

    @NotNull(message = "是否允许并发不能为空")
    @Schema(description = "是否允许并发", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean concurrent = Boolean.TRUE;

    @Positive(message = "默认超时秒数必须大于0")
    @Schema(description = "默认超时秒数")
    private Integer timeoutSeconds;

    @Size(max = 4096, message = "默认重试策略不能超过4096个字符")
    @Schema(description = "默认重试策略 JSON")
    private String retryPolicy;
}
