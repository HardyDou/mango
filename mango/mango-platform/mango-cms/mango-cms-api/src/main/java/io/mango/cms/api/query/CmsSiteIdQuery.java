package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CmsSiteIdQuery {

    @NotNull(message = "站点 ID 不能为空")
    @Schema(description = "站点 ID")
    private Long siteId;
}
