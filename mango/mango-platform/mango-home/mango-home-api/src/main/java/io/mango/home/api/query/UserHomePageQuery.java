package io.mango.home.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户自定义首页分页查询")
public class UserHomePageQuery extends PageQuery {

    @Schema(description = "关键词。支持首页名称、路由标识模糊搜索")
    private String keyword;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "启用状态")
    private Boolean enabled;
}
