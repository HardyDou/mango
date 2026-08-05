package io.mango.workflow.api.command;

import io.mango.workflow.api.validation.WorkflowOptionalValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 撤回运行中业务审批流程命令。
 */
@Data
@Schema(description = "撤回运行中业务审批流程命令")
public class WithdrawWorkflowProcessCommand {

    @Schema(description = "业务申请ID，与流程实例ID至少填写一个")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Long applyId;

    @Schema(description = "流程实例ID，与业务申请ID至少填写一个")
    @Size(max = 128, message = "流程实例ID最多128个字符")
    private String processInstanceId;

    @Schema(description = "撤回原因")
    @NotBlank(message = "撤回原因不能为空")
    @Size(max = 1000, message = "撤回原因最多1000个字符")
    private String reason;
}
