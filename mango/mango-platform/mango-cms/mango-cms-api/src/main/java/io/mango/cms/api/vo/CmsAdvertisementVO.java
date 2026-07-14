package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsAdvertisementVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "站点 ID")
    private Long siteId;
    @Schema(description = "广告编码")
    private String adCode;
    @Schema(description = "广告名称")
    private String adName;
    @Schema(description = "展示位置")
    private String position;
    @Schema(description = "位置类型")
    private String positionType;
    @Schema(description = "支持的素材类型列表")
    private String supportedMaterialTypes;
    @Schema(description = "宽度")
    private Integer width;
    @Schema(description = "高度")
    private Integer height;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "排序值")
    private Integer sort;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
