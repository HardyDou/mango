package io.mango.job.api.command;

import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.enums.JobWorkerRegisterSource;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Mango Job Worker 注册命令。
 */
@Data
@Schema(description = "Mango Job Worker 注册命令")
public class RegisterMangoJobWorkerCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 不能超过64个字符")
    @Schema(description = "租户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantId;

    @NotBlank(message = "所属应用不能为空")
    @Size(max = 128, message = "所属应用不能超过128个字符")
    @Schema(description = "Worker 所属逻辑应用", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @Size(max = 128, message = "执行服务编码不能超过128个字符")
    @Schema(description = "执行服务编码。为空时默认使用所属应用")
    private String serviceCode;

    @Size(max = 128, message = "Worker 分组不能超过128个字符")
    @Schema(description = "Worker 分组。为空时默认使用执行服务编码")
    private String workerGroup;

    @NotBlank(message = "Worker 地址不能为空")
    @Size(max = 256, message = "Worker 地址不能超过256个字符")
    @Schema(description = "Worker 执行地址。内嵌为 embedded://，兼容旧 in-memory://，远程为 http(s)://", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workerAddress;

    @Size(max = 256, message = "运行地址不能超过256个字符")
    @Schema(description = "运行地址。默认等于 Worker 地址")
    private String runtimeAddress;

    @NotNull(message = "通信方式不能为空")
    @Schema(description = "Worker 通信方式", requiredMode = Schema.RequiredMode.REQUIRED)
    private JobTransportType transportType;

    @Schema(description = "Worker 注册来源")
    private JobWorkerRegisterSource registerSource;

    @Size(max = 128, message = "Worker 实例标识不能超过128个字符")
    @Schema(description = "Worker 实例标识")
    private String workerInstanceId;

    @NotEmpty(message = "Worker 处理器清单不能为空")
    @Valid
    @Schema(description = "Worker 支持的处理器清单", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<MangoJobHandlerVO> handlers = new ArrayList<>();
}
