package io.mango.cms.api.query;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CmsSiteCategoryTreeQuery {

    @NotNull(message = "站点 ID 不能为空")
    private Long siteId;

    @Pattern(regexp = "|ENABLED|DISABLED", message = "状态不合法")
    private String status;
}
