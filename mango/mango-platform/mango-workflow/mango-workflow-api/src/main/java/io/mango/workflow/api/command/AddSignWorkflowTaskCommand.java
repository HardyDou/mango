package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 加签审批任务命令。
 */
@Data
@Schema(description = "加签审批任务命令")
public class AddSignWorkflowTaskCommand {

    @Schema(description = "任务ID")
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Schema(description = "加签办理人ID或用户名")
    @NotEmpty(message = "加签办理人不能为空")
    private List<String> targetUserIds;

    @Schema(description = "加签说明")
    @Size(max = 1000, message = "加签说明最多1000个字符")
    private String comment;
}
