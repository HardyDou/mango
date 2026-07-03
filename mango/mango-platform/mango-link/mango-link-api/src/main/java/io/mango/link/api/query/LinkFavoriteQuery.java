package io.mango.link.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 我的收藏查询。
 */
@Data
@Schema(description = "我的收藏查询")
public class LinkFavoriteQuery {

    @Size(max = 128, message = "关键词最多128个字符")
    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "分类 ID")
    private Long categoryId;
}
