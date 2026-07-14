package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsContentPageQuery extends CmsBasePageQuery {

    @Schema(description = "分类 ID")
    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;

    @Pattern(regexp = "|ARTICLE|IMAGE_TEXT|PAGE|ATTACHMENT|VIDEO", message = "内容类型不合法")
    @Schema(description = "内容类型")
    private String contentType;
}
