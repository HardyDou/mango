package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SiteNavigationQuery extends SiteBaseQuery {

    @Pattern(regexp = "|TOP|FOOTER|QUICK", message = "导航类型不合法")
    @Schema(description = "导航类型")
    private String navType;
}
