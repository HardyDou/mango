package io.mango.job.api.command;

import io.mango.job.api.enums.JobWorkerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新 Mango Job Worker 治理状态命令。
 */
@Data
@Schema(description = "更新 Mango Job Worker 治理状态命令")
public class UpdateMangoJobWorkerStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Worker ID 不能为空")
    @Schema(description = "Worker 快照 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "Worker 状态不能为空")
    @Schema(description = "目标 Worker 状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private JobWorkerStatus status;
}
