package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 从流程模板创建流程定义命令。
 */
@Data
@Schema(description = "从流程模板创建流程定义命令")
public class CreateWorkflowDefinitionFromTemplateCommand {

    @Schema(description = "流程模板ID")
    @NotNull(message = "流程模板ID不能为空")
    private Long templateId;

    @Schema(description = "流程分类ID")
    @NotNull(message = "流程分类ID不能为空")
    private Long categoryId;

    @Schema(description = "目标租户ID；为空时使用当前登录租户")
    private Long targetTenantId;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    @Size(max = 128, message = "流程名称最多128个字符")
    private String definitionName;

    @Schema(description = "流程编码")
    @NotBlank(message = "流程编码不能为空")
    @Size(max = 128, message = "流程编码最多128个字符")
    private String definitionKey;

    @Schema(description = "流程管理员用户名列表")
    private List<String> adminUsers;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多255个字符")
    private String remark;
}
