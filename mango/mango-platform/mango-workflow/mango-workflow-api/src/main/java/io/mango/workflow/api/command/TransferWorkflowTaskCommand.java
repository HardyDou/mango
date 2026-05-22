package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 转办审批任务命令。
 */
@Data
@Schema(description = "转办审批任务命令")
public class TransferWorkflowTaskCommand {

    @Schema(description = "任务ID")
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Schema(description = "目标办理人ID或用户名")
    @NotBlank(message = "目标办理人不能为空")
    private String targetUserId;

    @Schema(description = "转办说明")
    @Size(max = 1000, message = "转办说明最多1000个字符")
    private String comment;
}
