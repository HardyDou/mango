package io.mango.job.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Job 处理器元数据返回对象。
 */
@Data
@Schema(description = "Job 处理器元数据返回对象")
public class MangoJobHandlerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "所属逻辑应用")
    private String appCode;

    @Schema(description = "执行服务编码")
    private String serviceCode;

    @Schema(description = "Worker 分组")
    private String workerGroup;

    @Schema(description = "处理器名称")
    private String handlerName;

    @Schema(description = "支持的任务编码。为空表示不限制 jobCode")
    private Set<String> supportedJobCodes = new LinkedHashSet<>();

    @Schema(description = "处理器类型")
    private String jobType;

    @Schema(description = "参数表单 schema JSON")
    private String paramSchema;

    @Schema(description = "是否允许并发")
    private Boolean concurrent;

    @Schema(description = "默认超时秒数")
    private Integer timeoutSeconds;

    @Schema(description = "默认重试策略 JSON")
    private String retryPolicy;
}
