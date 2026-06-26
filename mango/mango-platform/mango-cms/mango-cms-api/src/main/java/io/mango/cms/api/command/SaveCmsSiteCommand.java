package io.mango.cms.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsSiteCommand {

    private Long id;

    @NotBlank(message = "站点名称不能为空")
    @Size(max = 128, message = "站点名称最多128个字符")
    private String siteName;

    @NotBlank(message = "站点编码不能为空")
    @Size(max = 64, message = "站点编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "站点编码只能包含字母、数字、点、下划线、冒号和短横线")
    private String siteCode;

    @Size(max = 128, message = "Logo 文件 ID 最多128个字符")
    private String logoFileId;

    @Size(max = 512, message = "站点描述最多512个字符")
    private String description;

    @Size(max = 255, message = "站点域名最多255个字符")
    private String domain;

    @Size(max = 32, message = "默认语言最多32个字符")
    private String defaultLanguage;

    @Size(max = 255, message = "SEO 标题最多255个字符")
    private String seoTitle;

    @Size(max = 512, message = "SEO 关键词最多512个字符")
    private String seoKeywords;

    @Size(max = 1024, message = "SEO 描述最多1024个字符")
    private String seoDescription;

    @Size(max = 512, message = "版权信息最多512个字符")
    private String footerCopyright;

    @Size(max = 255, message = "备案号最多255个字符")
    private String icpRecord;

    @Size(max = 1024, message = "联系方式最多1024个字符")
    private String contactInfo;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    private String status;
}
