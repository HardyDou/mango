package io.mango.cms.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsSiteCategoryCommand {

    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    private Long siteId;

    private Long parentId;

    @NotBlank(message = "栏目名称不能为空")
    @Size(max = 128, message = "栏目名称最多128个字符")
    private String categoryName;

    @NotBlank(message = "栏目编码不能为空")
    @Size(max = 64, message = "栏目编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "栏目编码只能包含字母、数字、点、下划线、冒号和短横线")
    private String categoryCode;

    @NotBlank(message = "栏目类型不能为空")
    @Pattern(regexp = "LIST|PAGE|LINK", message = "栏目类型不合法")
    private String categoryType;

    @Size(max = 255, message = "访问路径最多255个字符")
    private String accessPath;

    @Size(max = 512, message = "外部地址最多512个字符")
    private String externalUrl;

    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "可见状态不合法")
    private String visibleStatus;

    @Pattern(regexp = "PUBLIC|LOGIN|ROLE", message = "访问权限不合法")
    private String accessType;

    @Size(max = 512, message = "角色编码最多512个字符")
    private String roleCodes;

    @Size(max = 255, message = "SEO 标题最多255个字符")
    private String seoTitle;

    @Size(max = 512, message = "SEO 关键词最多512个字符")
    private String seoKeywords;

    @Size(max = 1024, message = "SEO 描述最多1024个字符")
    private String seoDescription;
}
