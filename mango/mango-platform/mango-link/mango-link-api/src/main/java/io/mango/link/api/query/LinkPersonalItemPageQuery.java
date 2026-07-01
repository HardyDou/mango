package io.mango.link.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的网址分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "我的网址分页查询")
public class LinkPersonalItemPageQuery extends PageQuery {

    @Size(max = 128, message = "关键词最多128个字符")
    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "分类 ID")
    private Long categoryId;
}
