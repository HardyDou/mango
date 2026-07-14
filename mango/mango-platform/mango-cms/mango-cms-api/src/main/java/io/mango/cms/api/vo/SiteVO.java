package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SiteVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "站点名称")
    private String siteName;
    @Schema(description = "站点编码")
    private String siteCode;
    @Schema(description = "Logo 文件 ID")
    private String logoFileId;
    @Schema(description = "Logo 地址")
    private String logoUrl;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "站点域名")
    private String domain;
    @Schema(description = "默认语言")
    private String defaultLanguage;
    @Schema(description = "SEO 标题")
    private String seoTitle;
    @Schema(description = "SEO 关键词")
    private String seoKeywords;
    @Schema(description = "SEO 描述")
    private String seoDescription;
    @Schema(description = "页脚版权")
    private String footerCopyright;
    @Schema(description = "ICP备案信息")
    private String icpRecord;
    @Schema(description = "联系信息")
    private String contactInfo;
}
