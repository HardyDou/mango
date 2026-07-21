package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdminBrandingVO {

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "后台名称")
    private String title;

    @Schema(description = "后台简称")
    private String shortTitle;

    @Schema(description = "后台副标题")
    private String subtitle;

    @Schema(description = "登录页标题")
    private String loginTitle;

    @Schema(description = "登录页副标题")
    private String loginSubtitle;

    @Schema(description = "Logo 文件标识")
    private String logoFile;

    @Schema(description = "折叠 Logo 文件标识")
    private String logoIconFile;

    @Schema(description = "Favicon 文件标识")
    private String faviconFile;

    @Schema(description = "登录页图片文件标识")
    private String loginImageFile;

    @Schema(description = "页脚版权")
    private String footerCopyright;

    @Schema(description = "备案号")
    private String icp;

    @Schema(description = "联系人")
    private String contact;
}
