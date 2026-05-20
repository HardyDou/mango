package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存流程分类命令。
 */
@Data
@Schema(description = "保存流程分类命令")
public class SaveWorkflowCategoryCommand {

    @Schema(description = "流程分类ID，新增时为空，修改时必填")
    private Long id;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称最多64个字符")
    private String categoryName;

    @Schema(description = "分类编码")
    @NotBlank(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码最多64个字符")
    private String categoryCode;

    @Schema(description = "排序号，越小越靠前")
    private Integer sort;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多255个字符")
    private String remark;
}
