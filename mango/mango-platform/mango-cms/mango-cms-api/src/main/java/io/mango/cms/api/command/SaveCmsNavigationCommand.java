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
public class SaveCmsNavigationCommand {

    @Schema(description = "主键 ID")
    @Positive(message = "主键 ID 必须大于 0")
    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    @Schema(description = "站点 ID")
    private Long siteId;

    @NotBlank(message = "导航类型不能为空")
    @Pattern(regexp = "TOP|FOOTER|QUICK", message = "导航类型不合法")
    @Schema(description = "导航类型")
    private String navType;

    @NotBlank(message = "导航名称不能为空")
    @Size(max = 128, message = "导航名称最多128个字符")
    @Schema(description = "导航名称")
    private String navName;

    @NotBlank(message = "跳转类型不能为空")
    @Pattern(regexp = "CATEGORY|CONTENT|URL", message = "跳转类型不合法")
    @Schema(description = "跳转类型")
    private String jumpType;

    @Schema(description = "分类 ID")
    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;

    @Schema(description = "内容 ID")
    @Positive(message = "内容 ID 必须大于 0")
    private Long contentId;

    @Size(max = 512, message = "外部地址最多512个字符")
    @Schema(description = "外部地址")
    private String externalUrl;

    @Pattern(regexp = "SELF|BLANK", message = "打开方式不合法")
    @Schema(description = "打开目标")
    private String openTarget;

    @Schema(description = "排序值")
    @PositiveOrZero(message = "排序值不能小于 0")
    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    @Schema(description = "状态")
    private String status;
}
