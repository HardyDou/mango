package io.mango.template.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板分类分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模板分类分页查询")
public class TemplateCategoryPageQuery extends PageQuery {

    @Schema(description = "关键词，匹配分类名称或编码")
    private String keyword;

    @Schema(description = "分类状态：0停用，1启用")
    private Integer status;
}
