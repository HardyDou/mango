package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsNavigationVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "站点 ID")
    private Long siteId;
    @Schema(description = "导航类型")
    private String navType;
    @Schema(description = "导航名称")
    private String navName;
    @Schema(description = "跳转类型")
    private String jumpType;
    @Schema(description = "分类 ID")
    private Long categoryId;
    @Schema(description = "内容 ID")
    private Long contentId;
    @Schema(description = "外部地址")
    private String externalUrl;
    @Schema(description = "打开目标")
    private String openTarget;
    @Schema(description = "排序值")
    private Integer sort;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
