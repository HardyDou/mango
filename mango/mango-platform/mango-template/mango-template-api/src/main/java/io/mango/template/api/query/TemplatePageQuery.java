package io.mango.template.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模板分页查询")
public class TemplatePageQuery extends PageQuery {

    @Schema(description = "关键词，匹配模板名称或编码")
    private String keyword;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Deprecated
    @Schema(description = "业务组编码。兼容历史字段，前端不再使用")
    private String businessGroup;

    @Deprecated
    @Schema(description = "业务类型。兼容历史字段，前端不再使用")
    private String businessType;

    @Deprecated
    @Schema(description = "业务KEY。兼容历史字段，新调用统一使用模板编码")
    private String businessKey;

    @Schema(description = "模板源格式：TEXT、HTML、DOCX、XLSX")
    private String sourceFormat;

    @Schema(description = "模板状态：0停用，1启用")
    private Integer status;
}
