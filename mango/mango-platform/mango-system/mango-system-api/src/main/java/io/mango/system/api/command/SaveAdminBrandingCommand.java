package io.mango.system.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveAdminBrandingCommand {

    @Schema(description = "是否启用")
    @Valid
    @NotNull(message = "品牌配置启用状态不能为空")
    private Boolean enabled;

    @Size(max = 100, message = "后台名称长度不能超过100")
    @Schema(description = "后台名称")
    private String title;

    @Size(max = 50, message = "后台简称长度不能超过50")
    @Schema(description = "后台简称")
    private String shortTitle;

    @Size(max = 200, message = "后台副标题长度不能超过200")
    @Schema(description = "后台副标题")
    private String subtitle;

    @Size(max = 100, message = "登录页标题长度不能超过100")
    @Schema(description = "登录页标题")
    private String loginTitle;

    @Size(max = 200, message = "登录页副标题长度不能超过200")
    @Schema(description = "登录页副标题")
    private String loginSubtitle;

    @Size(max = 100, message = "Logo 文件标识长度不能超过100")
    @Schema(description = "Logo 文件标识")
    private String logoFile;

    @Size(max = 100, message = "折叠 Logo 文件标识长度不能超过100")
    @Schema(description = "折叠 Logo 文件标识")
    private String logoIconFile;

    @Size(max = 100, message = "favicon 文件标识长度不能超过100")
    @Schema(description = "Favicon 文件标识")
    private String faviconFile;

    @Size(max = 100, message = "登录页图片文件标识长度不能超过100")
    @Schema(description = "登录页图片文件标识")
    private String loginImageFile;

    @Size(max = 200, message = "页脚版权长度不能超过200")
    @Schema(description = "页脚版权")
    private String footerCopyright;

    @Size(max = 100, message = "备案号长度不能超过100")
    @Schema(description = "备案号")
    private String icp;

    @Size(max = 100, message = "联系方式长度不能超过100")
    @Schema(description = "联系人")
    private String contact;
}
