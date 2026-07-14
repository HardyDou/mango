package io.mango.cms.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsSiteCategoryCommand {

    @Schema(description = "主键 ID")
    @Positive(message = "主键 ID 必须大于 0")
    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    @Schema(description = "站点 ID")
    private Long siteId;

    @Schema(description = "父级 ID")
    @PositiveOrZero(message = "父级 ID 不能小于 0")
    private Long parentId;

    @NotBlank(message = "栏目名称不能为空")
    @Size(max = 128, message = "栏目名称最多128个字符")
    @Schema(description = "分类名称")
    private String categoryName;

    @NotBlank(message = "栏目编码不能为空")
    @Size(max = 64, message = "栏目编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "栏目编码只能包含字母、数字、点、下划线、冒号和短横线")
    @Schema(description = "分类编码")
    private String categoryCode;

    @NotBlank(message = "栏目类型不能为空")
    @Pattern(regexp = "LIST|PAGE|LINK", message = "栏目类型不合法")
    @Schema(description = "分类类型")
    private String categoryType;

    @Size(max = 255, message = "访问路径最多255个字符")
    @Schema(description = "访问路径")
    private String accessPath;

    @Size(max = 512, message = "外部地址最多512个字符")
    @Schema(description = "外部地址")
    private String externalUrl;

    @Schema(description = "排序值")
    @PositiveOrZero(message = "排序值不能小于 0")
    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "可见状态不合法")
    @Schema(description = "显示状态")
    private String visibleStatus;

    @Pattern(regexp = "PUBLIC|LOGIN|ROLE", message = "访问权限不合法")
    @Schema(description = "访问类型")
    private String accessType;

    @Size(max = 512, message = "角色编码最多512个字符")
    @Schema(description = "角色编码列表")
    private String roleCodes;

    @Size(max = 255, message = "SEO 标题最多255个字符")
    @Schema(description = "SEO 标题")
    private String seoTitle;

    @Size(max = 512, message = "SEO 关键词最多512个字符")
    @Schema(description = "SEO 关键词")
    private String seoKeywords;

    @Size(max = 1024, message = "SEO 描述最多1024个字符")
    @Schema(description = "SEO 描述")
    private String seoDescription;
}
