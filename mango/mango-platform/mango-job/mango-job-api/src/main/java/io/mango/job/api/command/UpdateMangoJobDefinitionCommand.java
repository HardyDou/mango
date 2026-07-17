package io.mango.job.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新 Job 任务定义命令。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "更新 Job 任务定义命令")
public class UpdateMangoJobDefinitionCommand extends CreateMangoJobDefinitionCommand {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务 ID 不能为空")
    @Positive(message = "任务 ID 必须大于0")
    @Schema(description = "任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
