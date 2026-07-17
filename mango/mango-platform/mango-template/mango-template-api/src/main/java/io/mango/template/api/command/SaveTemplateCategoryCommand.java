package io.mango.template.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建和修改模板分类共享的保存字段。
 */
@Data
@Schema(description = "模板分类保存字段")
public class SaveTemplateCategoryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码不能超过64个字符")
    @Schema(description = "分类编码")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 128, message = "分类名称不能超过128个字符")
    @Schema(description = "分类名称")
    private String categoryName;

    @Min(value = 0, message = "排序不能小于0")
    @Schema(description = "排序")
    private Integer sort;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态：0停用，1启用")
    private Integer status;

    @Size(max = 255, message = "备注不能超过255个字符")
    @Schema(description = "备注")
    private String remark;
}
