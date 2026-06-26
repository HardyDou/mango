package io.mango.cms.api.query;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CmsSiteIdQuery {

    @NotNull(message = "站点 ID 不能为空")
    private Long siteId;
}
