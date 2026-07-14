package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.common.po.PageQuery;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsBasePageQuery extends PageQuery {

    @Size(max = 128, message = "关键词最多128个字符")
    @Schema(description = "关键词")
    private String keyword;

    @Pattern(regexp = "|ENABLED|DISABLED|DRAFT|PENDING_REVIEW|REJECTED|PUBLISHED|OFFLINE", message = "状态不合法")
    @Schema(description = "状态")
    private String status;
}
