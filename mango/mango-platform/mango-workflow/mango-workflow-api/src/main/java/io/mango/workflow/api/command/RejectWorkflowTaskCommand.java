package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 驳回审批任务命令。
 */
@Data
@Schema(description = "驳回审批任务命令")
public class RejectWorkflowTaskCommand {

    @Schema(description = "任务ID")
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Schema(description = "驳回原因")
    @Size(max = 1000, message = "驳回原因最多1000个字符")
    private String comment;

    @Schema(description = "驳回时提交的变量")
    private Map<String, Object> variables;
}
