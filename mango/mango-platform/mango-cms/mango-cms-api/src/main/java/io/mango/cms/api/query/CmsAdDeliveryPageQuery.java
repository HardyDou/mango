package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsAdDeliveryPageQuery extends CmsBasePageQuery {

    @Schema(description = "站点 ID")
    @Positive(message = "站点 ID 必须大于 0")
    private Long siteId;

    @Schema(description = "广告位 ID")
    @Positive(message = "广告位 ID 必须大于 0")
    private Long adId;

    @Schema(description = "素材类型")
    @Pattern(regexp = "|TEXT|RICH_TEXT|HTML|IMAGE|SINGLE_IMAGE|MULTI_IMAGE|VIDEO", message = "素材类型不合法")
    private String materialType;
}
