package io.mango.home.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "首页视图")
public class HomePageVO implements Serializable {

    @Schema(description = "用户首页ID；内置或模板首页为空")
    private Long id;

    @Schema(description = "首页路由标识")
    private String routeKey;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "首页所属用户ID")
    private Long userId;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板版本ID")
    private Long templateVersionId;

    @Schema(description = "首页名称")
    private String name;

    @Schema(description = "布局JSON")
    private String layoutJson;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "是否当前默认首页")
    private Boolean defaultPage;

    @Schema(description = "是否内置首页")
    private Boolean builtIn;

    @Schema(description = "首页来源类型")
    private String sourceType;

    @Schema(description = "首页来源说明")
    private String sourceLabel;

    @Schema(description = "首页来源说明列表")
    private List<String> sourceLabels;

    @Schema(description = "是否只读")
    private Boolean readOnly;

    @Schema(description = "是否允许复制")
    private Boolean canCopy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
