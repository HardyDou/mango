package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改流程定义状态命令。
 */
@Data
@Schema(description = "修改流程定义状态命令")
public class UpdateWorkflowDefinitionStatusCommand {

    @Schema(description = "流程定义ID")
    @NotNull(message = "流程定义ID不能为空")
    private Long id;

    @Schema(description = "目标状态：DRAFT-草稿，PUBLISHED-已发布，DISABLED-停用")
    @NotBlank(message = "目标状态不能为空")
    private String status;
}
