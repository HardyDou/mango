package io.mango.cms.api.query;

import io.mango.common.po.PageQuery;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SiteContentPageQuery extends PageQuery {

    @Size(max = 64, message = "站点编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "站点编码只能包含字母、数字、点、下划线、冒号和短横线")
    private String siteCode;

    @Size(max = 255, message = "站点域名最多255个字符")
    private String domain;

    private Long categoryId;

    @Size(max = 128, message = "关键词最多128个字符")
    private String keyword;

    @Pattern(regexp = "|NONE|HOME|HOT|EDITOR", message = "推荐类型不合法")
    private String recommendationType;
}
