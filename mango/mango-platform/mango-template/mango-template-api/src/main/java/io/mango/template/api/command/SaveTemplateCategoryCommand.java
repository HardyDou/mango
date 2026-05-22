package io.mango.template.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存模板分类命令。
 */
@Data
@Schema(description = "保存模板分类命令")
public class SaveTemplateCategoryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板分类ID，修改时必填")
    private Long id;

    @NotBlank
    @Schema(description = "分类编码")
    private String categoryCode;

    @NotBlank
    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0停用，1启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
