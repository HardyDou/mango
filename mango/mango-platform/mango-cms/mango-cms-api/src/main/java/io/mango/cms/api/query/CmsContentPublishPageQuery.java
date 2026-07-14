package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsContentPublishPageQuery extends CmsBasePageQuery {

    @Schema(description = "内容 ID")
    @Positive(message = "内容 ID 必须大于 0")
    private Long contentId;

    @Schema(description = "站点 ID")
    @Positive(message = "站点 ID 必须大于 0")
    private Long siteId;

    @Schema(description = "分类 ID")
    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;
}
