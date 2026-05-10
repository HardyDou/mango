package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改流程节点定义状态命令。
 */
@Data
@Schema(description = "修改流程节点定义状态命令")
public class UpdateWorkflowNodeDefinitionStatusCommand {

    @Schema(description = "节点定义ID")
    @NotNull(message = "节点定义ID不能为空")
    private Long id;

    @Schema(description = "目标状态：0-停用，1-启用")
    @NotNull(message = "目标状态不能为空")
    private Integer status;
}
