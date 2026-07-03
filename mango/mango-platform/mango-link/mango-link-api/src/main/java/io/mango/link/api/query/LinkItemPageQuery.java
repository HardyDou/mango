package io.mango.link.api.query;

import io.mango.common.po.PageQuery;
import io.mango.link.api.enums.LinkStatus;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "可见范围")
    private LinkVisibilityScope visibilityScope;

    @Schema(description = "状态")
    private LinkStatus status;
}
