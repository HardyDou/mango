package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 改派审批任务命令。
 *
 * <p>管理员把任务改派给具体办理人，或把任务回收进节点候选池。{@code targetUserId} 与 {@code pool}
 * 二选一：指定人时写入新的承办人；{@code pool=true} 时清空承办人并把任务交回候选池。
 */
@Data
@Schema(description = "改派审批任务命令")
public class ReassignWorkflowTaskCommand {

    @Schema(description = "任务ID")
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Schema(description = "目标办理人ID或用户名；pool=true 时忽略")
    @Size(max = 128, message = "目标办理人最多128个字符")
    private String targetUserId;

    @Schema(description = "是否回收进节点候选池；与 targetUserId 二选一")
    private Boolean pool;

    @Schema(description = "改派说明")
    @Size(max = 1000, message = "改派说明最多1000个字符")
    private String comment;
}
