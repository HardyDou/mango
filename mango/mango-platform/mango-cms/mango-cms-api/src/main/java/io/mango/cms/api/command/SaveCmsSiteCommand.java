package io.mango.cms.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsSiteCommand {

    @Schema(description = "主键 ID")
    @Positive(message = "主键 ID 必须大于 0")
    private Long id;

    @NotBlank(message = "站点名称不能为空")
    @Size(max = 128, message = "站点名称最多128个字符")
    @Schema(description = "站点名称")
    private String siteName;

    @NotBlank(message = "站点编码不能为空")
    @Size(max = 64, message = "站点编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "站点编码只能包含字母、数字、点、下划线、冒号和短横线")
    @Schema(description = "站点编码")
    private String siteCode;

    @Size(max = 128, message = "Logo 文件 ID 最多128个字符")
    @Schema(description = "Logo 文件 ID")
    private String logoFileId;

    @Size(max = 512, message = "站点描述最多512个字符")
    @Schema(description = "描述")
    private String description;

    @Size(max = 255, message = "站点域名最多255个字符")
    @Schema(description = "站点域名")
    private String domain;

    @Size(max = 32, message = "默认语言最多32个字符")
    @Schema(description = "默认语言")
    private String defaultLanguage;

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

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    @Schema(description = "状态")
    private String status;
}
