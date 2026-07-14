package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SiteBaseQuery {

    @Size(max = 64, message = "站点编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "站点编码只能包含字母、数字、点、下划线、冒号和短横线")
    @Schema(description = "站点编码")
    private String siteCode;

    @Size(max = 255, message = "站点域名最多255个字符")
    @Schema(description = "站点域名")
    private String domain;
}
