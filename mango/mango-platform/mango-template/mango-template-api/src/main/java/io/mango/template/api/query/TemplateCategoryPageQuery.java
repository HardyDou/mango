package io.mango.template.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板分类分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模板分类分页查询")
public class TemplateCategoryPageQuery extends PageQuery {

    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "关键词，匹配分类名称或编码")
    private String keyword;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "分类状态：0停用，1启用")
    private Integer status;
}
