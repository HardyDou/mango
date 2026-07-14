package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CmsSiteCategoryTreeQuery {

    @NotNull(message = "站点 ID 不能为空")
    @Schema(description = "站点 ID")
    private Long siteId;

    @Pattern(regexp = "|ENABLED|DISABLED", message = "状态不合法")
    @Schema(description = "状态")
    private String status;
}
