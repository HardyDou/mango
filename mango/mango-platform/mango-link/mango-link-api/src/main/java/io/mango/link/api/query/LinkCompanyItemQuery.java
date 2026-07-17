package io.mango.link.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 公司网址查询。
 */
@Data
@Schema(description = "公司网址查询")
public class LinkCompanyItemQuery {

    @Size(max = 128, message = "关键词最多128个字符")
    @Schema(description = "关键词，匹配名称、URL、简介、标签")
    private String keyword;

    @Positive(message = "分类 ID 必须大于0")
    @Schema(description = "分类 ID")
    private Long categoryId;
}
