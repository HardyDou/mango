package io.mango.job.api.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新 Job 任务定义状态命令。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "更新 Job 任务定义状态命令")
public class UpdateMangoJobDefinitionStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务 ID 不能为空")
    @Schema(description = "任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "任务状态不能为空")
    @Size(max = 32, message = "任务状态不能超过32个字符")
    @Schema(description = "任务状态：DRAFT、ENABLED、DISABLED、PAUSED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}
