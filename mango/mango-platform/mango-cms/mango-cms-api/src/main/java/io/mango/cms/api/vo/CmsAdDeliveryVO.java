package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsAdDeliveryVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "站点 ID")
    private Long siteId;
    @Schema(description = "广告位 ID")
    private Long adId;
    @Schema(description = "广告名称")
    private String adName;
    @Schema(description = "广告编码")
    private String adCode;
    @Schema(description = "展示位置")
    private String position;
    @Schema(description = "位置类型")
    private String positionType;
    @Schema(description = "投放名称")
    private String deliveryName;
    @Schema(description = "素材类型")
    private String materialType;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "文本内容")
    private String textContent;
    @Schema(description = "富文本内容")
    private String richContent;
    @Schema(description = "HTML 内容")
    private String htmlContent;
    @Schema(description = "图片文件 ID")
    private String imageFileId;
    @Schema(description = "图片文件 ID 列表")
    private String imageFileIds;
    @Schema(description = "视频文件 ID")
    private String videoFileId;
    @Schema(description = "封面文件 ID")
    private String coverFileId;
    @Schema(description = "跳转地址")
    private String jumpUrl;
    @Schema(description = "打开目标")
    private String openTarget;
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
