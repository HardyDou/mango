package io.mango.link.api.query;

import io.mango.common.po.PageQuery;
import io.mango.link.api.enums.LinkStatus;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import io.mango.link.api.validation.LinkStrictValidation;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台网址分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台网址分页查询")
public class LinkItemPageQuery extends PageQuery {

    @Size(max = 128, message = "关键词最多128个字符")
    @Schema(description = "关键词")
    private String keyword;

    @Positive(message = "分类 ID 必须大于0")
    @Schema(description = "分类 ID")
    private Long categoryId;

    @NotNull(groups = LinkStrictValidation.class, message = "严格查询时可见范围不能为空")
    @Schema(description = "可见范围")
    private LinkVisibilityScope visibilityScope;

    @NotNull(groups = LinkStrictValidation.class, message = "严格查询时状态不能为空")
    @Schema(description = "状态")
    private LinkStatus status;
}
