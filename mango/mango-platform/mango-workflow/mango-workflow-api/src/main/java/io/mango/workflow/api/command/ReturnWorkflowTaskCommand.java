package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 退回审批任务命令。
 */
@Data
@Schema(description = "退回审批任务命令")
public class ReturnWorkflowTaskCommand {

    @Schema(description = "任务ID")
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Schema(description = "目标历史节点定义Key；为空时退回最近一个已完成的不同用户任务节点")
    @Size(max = 128, message = "目标节点定义Key最多128个字符")
    private String targetTaskDefinitionKey;

    @Schema(description = "退回意见")
    @Size(max = 1000, message = "退回意见最多1000个字符")
    private String comment;

    @Schema(description = "退回时提交的变量")
    private Map<String, Object> variables;
}
