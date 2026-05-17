package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
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

    @Schema(description = "业务类型")
    @Size(max = 128, message = "业务类型最多128个字符")
    private String businessType;

    @Schema(description = "业务申请ID")
    private Long applyId;

    @Schema(description = "申请审批渲染模式")
    private WorkflowApplyRenderMode renderMode;

    @Schema(description = "自定义申请页Key")
    @Size(max = 128, message = "自定义申请页Key最多128个字符")
    private String applyPageKey;

    @Schema(description = "自定义审批页Key")
    @Size(max = 128, message = "自定义审批页Key最多128个字符")
    private String approvePageKey;

    @Schema(description = "业务快照引用")
    @Size(max = 255, message = "业务快照引用最多255个字符")
    private String snapshotRef;

    @Schema(description = "发起表单变量")
    private Map<String, Object> variables;

    @Schema(description = "发起人自选审批人，key 为节点ID或节点定义Key，value 为用户ID/用户名数组")
    private Map<String, Object> selectedAssignees;
}
