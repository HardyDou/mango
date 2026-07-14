package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SiteResolveVO {
    @Schema(description = "站点 ID")
    private Long siteId;
    @Schema(description = "站点编码")
    private String siteCode;
    @Schema(description = "站点名称")
    private String siteName;
    @Schema(description = "状态")
    private String status;
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
