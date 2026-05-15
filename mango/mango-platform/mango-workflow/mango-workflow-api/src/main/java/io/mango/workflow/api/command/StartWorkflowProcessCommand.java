package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 发起流程命令。
 */
@Data
@Schema(description = "发起流程命令")
public class StartWorkflowProcessCommand {

    @Schema(description = "Mango流程定义ID")
    @NotNull(message = "流程定义ID不能为空")
    private Long definitionId;

    @Schema(description = "业务主键，可为空；为空时后端生成")
    @Size(max = 128, message = "业务主键最多128个字符")
    private String businessKey;

    @Schema(description = "发起表单变量")
    private Map<String, Object> variables;

    @Schema(description = "发起人自选审批人，key 为节点ID或节点定义Key，value 为用户ID/用户名数组")
    private Map<String, Object> selectedAssignees;
}
