package io.mango.link.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公开网址查询。
 */
@Data
@Schema(description = "公开网址查询")
public class LinkPublicItemQuery {

    @Schema(description = "租户上下文")
    private Long tenantId;

    @Size(max = 128, message = "关键词最多128个字符")
    @Schema(description = "关键词，匹配名称、URL、简介、标签")
    private String keyword;

    @Schema(description = "分类 ID")
    private Long categoryId;
}
