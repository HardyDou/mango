package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsNavigationPageQuery extends CmsBasePageQuery {

    @Schema(description = "站点 ID")
    @Positive(message = "站点 ID 必须大于 0")
    private Long siteId;

    @Pattern(regexp = "|TOP|FOOTER|QUICK", message = "导航类型不合法")
    @Schema(description = "导航类型")
    private String navType;
}
