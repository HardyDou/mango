package io.mango.cms.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsSiteSettingCommand {

    @NotNull(message = "站点 ID 不能为空")
    @Schema(description = "站点 ID")
    private Long siteId;

    @Size(max = 255, message = "SEO 标题最多255个字符")
    @Schema(description = "SEO 标题")
    private String seoTitle;

    @Size(max = 512, message = "SEO 关键词最多512个字符")
    @Schema(description = "SEO 关键词")
    private String seoKeywords;

    @Size(max = 1024, message = "SEO 描述最多1024个字符")
    @Schema(description = "SEO 描述")
    private String seoDescription;

    @Size(max = 512, message = "版权信息最多512个字符")
    @Schema(description = "页脚版权")
    private String footerCopyright;

    @Size(max = 255, message = "备案号最多255个字符")
    @Schema(description = "ICP备案信息")
    private String icpRecord;

    @Size(max = 1024, message = "联系方式最多1024个字符")
    @Schema(description = "联系信息")
    private String contactInfo;
}
