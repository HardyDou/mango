package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 从流程定义创建流程模板命令。
 */
@Data
@Schema(description = "从流程定义创建流程模板命令")
public class CreateWorkflowTemplateFromDefinitionCommand {

    @Schema(description = "流程定义ID")
    @NotNull(message = "流程定义ID不能为空")
    private Long definitionId;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称最多128个字符")
    private String templateName;

    @Schema(description = "模板编码")
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 128, message = "模板编码最多128个字符")
    private String templateCode;

    @Schema(description = "历史流程模板分类ID，业务域替换后前台不再使用")
    private Long templateCategoryId;

    @Schema(description = "业务域编码。为空时使用流程定义所属业务域")
    @Size(max = 64, message = "业务域编码最多64个字符")
    private String categoryCode;

    @Schema(description = "业务域名称")
    @Size(max = 64, message = "业务域名称最多64个字符")
    private String categoryName;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多255个字符")
    private String remark;
}
