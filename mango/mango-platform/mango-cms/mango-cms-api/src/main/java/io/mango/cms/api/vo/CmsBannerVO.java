package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsBannerVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "站点 ID")
    private Long siteId;
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
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @Schema(description = "排序值")
    private Integer sort;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
