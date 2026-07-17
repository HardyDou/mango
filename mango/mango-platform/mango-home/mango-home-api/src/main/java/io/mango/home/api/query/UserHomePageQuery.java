package io.mango.home.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户自定义首页分页查询")
public class UserHomePageQuery extends PageQuery {

    @Size(max = 64, message = "关键词长度不能超过64")
    @Schema(description = "关键词。支持首页名称、路由标识模糊搜索")
    private String keyword;

    @Min(value = 1, message = "用户ID必须大于0")
    @Schema(description = "用户ID")
    private Long userId;

    @Pattern(regexp = "(?i:true|false)", message = "启用状态必须为true或false")
    @Schema(description = "启用状态")
    private String enabled;
}
