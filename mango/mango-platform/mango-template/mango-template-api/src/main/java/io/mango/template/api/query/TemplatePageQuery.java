package io.mango.template.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 模板分页查询。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模板分页查询")
public class TemplatePageQuery extends PageQuery {

    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "关键词，匹配模板名称或编码")
    private String keyword;

    @Size(max = 64, message = "分类编码不能超过64个字符")
    @Schema(description = "分类编码")
    private String categoryCode;

    @Size(max = 64, message = "业务域编码不能超过64个字符")
    @Schema(description = "业务域编码")
    private String domainCode;

    @Deprecated
    @Size(max = 64, message = "业务组编码不能超过64个字符")
    @Schema(description = "业务组编码。兼容历史字段，前端不再使用")
    private String businessGroup;

    @Deprecated
    @Size(max = 64, message = "业务类型不能超过64个字符")
    @Schema(description = "业务类型。兼容历史字段，前端不再使用")
    private String businessType;

    @Deprecated
    @Size(max = 128, message = "业务KEY不能超过128个字符")
    @Schema(description = "业务KEY。兼容历史字段，新调用统一使用模板编码")
    private String businessKey;

    @Size(max = 32, message = "模板源格式不能超过32个字符")
    @Schema(description = "模板源格式：TEXT、HTML、DOCX、XLSX")
    private String sourceFormat;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "模板状态：0停用，1启用")
    private Integer status;
}
