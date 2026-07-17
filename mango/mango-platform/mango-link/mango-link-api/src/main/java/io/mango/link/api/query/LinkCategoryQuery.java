package io.mango.link.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import io.mango.link.api.validation.LinkStrictValidation;
import lombok.Data;

/**
 * 网址分类列表查询。
 */
@Data
@Schema(description = "网址分类列表查询")
public class LinkCategoryQuery {

    @Size(max = 128, message = "关键词最多128个字符")
    @Schema(description = "关键词")
    private String keyword;

    @NotNull(groups = LinkStrictValidation.class, message = "严格查询时停用分类标记不能为空")
    @Schema(description = "是否包含停用分类")
    private Boolean includeDisabled;
}
