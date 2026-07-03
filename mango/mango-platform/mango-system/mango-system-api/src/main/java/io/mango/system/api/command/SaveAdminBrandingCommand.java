package io.mango.system.api.command;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveAdminBrandingCommand {

    private Boolean enabled;

    @Size(max = 100, message = "后台名称长度不能超过100")
    private String title;

    @Size(max = 50, message = "后台简称长度不能超过50")
    private String shortTitle;

    @Size(max = 200, message = "后台副标题长度不能超过200")
    private String subtitle;

    @Size(max = 100, message = "登录页标题长度不能超过100")
    private String loginTitle;

    @Size(max = 200, message = "登录页副标题长度不能超过200")
    private String loginSubtitle;

    @Size(max = 100, message = "Logo 文件标识长度不能超过100")
    private String logoFile;

    @Size(max = 100, message = "favicon 文件标识长度不能超过100")
    private String faviconFile;

    @Size(max = 100, message = "登录页图片文件标识长度不能超过100")
    private String loginImageFile;

    @Size(max = 200, message = "页脚版权长度不能超过200")
    private String footerCopyright;

    @Size(max = 100, message = "备案号长度不能超过100")
    private String icp;

    @Size(max = 100, message = "联系方式长度不能超过100")
    private String contact;
}
