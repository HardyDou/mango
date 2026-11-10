package io.mango.workflow.api.command;

import jakarta.validation.constraints.NotNull;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;


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

    @Schema(description = "目标历史节点定义Key，指向当前流程实例中已完成的用户任务节点，不需要历史活动ID；传网关或汇聚时解析到上游最近的已完成用户任务；为空时退回最近一个已完成的不同用户任务节点")
    @Size(max = 128, message = "目标节点定义Key最多128个字符")
    private String targetTaskDefinitionKey;

    @Schema(description = "退回后目标任务的办理人，传用户ID（identity_user.id，推荐）或用户名；仅当本次退回在目标节点新建唯一任务时生效，目标用户必须是当前租户启用成员，否则退回整体失败")
    @Size(max = 64, message = "退回目标办理人最多64个字符")
    private String targetAssigneeUserId;

    @Schema(description = "退回意见")
    @Size(max = 1000, message = "退回意见最多1000个字符")
    private String comment;

    @Schema(description = "退回时提交的变量")
    @NotNull(groups = WorkflowOptionalValidation.class)
    @jakarta.validation.Valid
    private WorkflowJsonRequest variables;
}
