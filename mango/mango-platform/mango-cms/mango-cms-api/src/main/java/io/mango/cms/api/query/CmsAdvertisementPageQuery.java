package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsAdvertisementPageQuery extends CmsBasePageQuery {

    @Schema(description = "站点 ID")
    @Positive(message = "站点 ID 必须大于 0")
    private Long siteId;

    @Size(max = 64, message = "广告位置最多64个字符")
    @Schema(description = "展示位置")
    private String position;
}
