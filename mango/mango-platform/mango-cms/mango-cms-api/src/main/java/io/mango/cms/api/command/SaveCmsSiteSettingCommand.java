package io.mango.cms.api.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsSiteSettingCommand {

    @NotNull(message = "站点 ID 不能为空")
    private Long siteId;

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
}
