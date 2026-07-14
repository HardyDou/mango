package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SiteContentDetailQuery extends SiteBaseQuery {

    @NotNull(message = "内容 ID 不能为空")
    @Schema(description = "内容 ID")
    private Long contentId;

    @Schema(description = "分类 ID")
    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;
}
