package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SiteBannerQuery extends SiteBaseQuery {

    @Size(max = 64, message = "展示位置最多64个字符")
    @Schema(description = "展示位置")
    private String position;
}
