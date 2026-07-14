package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SiteBannerVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "展示位置")
    private String position;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "副标题")
    private String subtitle;
    @Schema(description = "媒体类型")
    private String mediaType;
    @Schema(description = "媒体文件 ID")
    private String mediaFileId;
    @Schema(description = "跳转地址")
    private String jumpUrl;
    @Schema(description = "排序值")
    private Integer sort;
}
