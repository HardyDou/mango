package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 认领审批任务命令。
 */
@Data
@Schema(description = "认领审批任务命令")
public class ClaimWorkflowTaskCommand {

    @Schema(description = "任务ID")
    @NotBlank(message = "任务ID不能为空")
    private String taskId;
}
