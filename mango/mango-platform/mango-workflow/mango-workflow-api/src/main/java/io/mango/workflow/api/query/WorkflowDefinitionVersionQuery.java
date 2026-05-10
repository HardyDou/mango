package io.mango.workflow.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 流程定义版本查询。
 */
@Data
@Schema(description = "流程定义版本查询")
public class WorkflowDefinitionVersionQuery {

    @Schema(description = "流程定义ID")
    @NotNull(message = "流程定义ID不能为空")
    private Long definitionId;
}
