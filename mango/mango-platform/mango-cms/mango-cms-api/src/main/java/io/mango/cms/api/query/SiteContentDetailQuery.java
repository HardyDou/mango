package io.mango.cms.api.query;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SiteContentDetailQuery extends SiteBaseQuery {

    @NotNull(message = "内容 ID 不能为空")
    private Long contentId;

    private Long categoryId;
}
