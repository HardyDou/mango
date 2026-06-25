package io.mango.cms.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsNavigationCommand {

    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    private Long siteId;

    @NotBlank(message = "导航类型不能为空")
    @Pattern(regexp = "TOP|FOOTER|QUICK", message = "导航类型不合法")
    private String navType;

    @NotBlank(message = "导航名称不能为空")
    @Size(max = 128, message = "导航名称最多128个字符")
    private String navName;

    @NotBlank(message = "跳转类型不能为空")
    @Pattern(regexp = "CATEGORY|CONTENT|URL", message = "跳转类型不合法")
    private String jumpType;

    private Long categoryId;

    private Long contentId;

    @Size(max = 512, message = "外部地址最多512个字符")
    private String externalUrl;

    @Pattern(regexp = "SELF|BLANK", message = "打开方式不合法")
    private String openTarget;

    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    private String status;
}
