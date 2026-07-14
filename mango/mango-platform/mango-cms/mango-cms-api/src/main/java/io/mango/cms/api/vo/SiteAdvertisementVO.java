package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SiteAdvertisementVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "广告编码")
    private String adCode;
    @Schema(description = "广告名称")
    private String adName;
    @Schema(description = "展示位置")
    private String position;
    @Schema(description = "位置类型")
    private String positionType;
    @Schema(description = "广告类型")
    private String adType;
    @Schema(description = "素材类型")
    private String materialType;
    @Schema(description = "素材文件 ID")
    private String materialFileId;
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
    @Schema(description = "图片地址")
    private String imageUrl;
    @Schema(description = "图片地址列表")
    private String imageUrls;
    @Schema(description = "视频文件 ID")
    private String videoFileId;
    @Schema(description = "封面文件 ID")
    private String coverFileId;
    @Schema(description = "视频地址")
    private String videoUrl;
    @Schema(description = "封面地址")
    private String coverUrl;
    @Schema(description = "跳转地址")
    private String jumpUrl;
    @Schema(description = "打开目标")
    private String openTarget;
    @Schema(description = "排序值")
    private Integer sort;
}
