package io.mango.cms.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CmsContentCategoryVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "父级 ID")
    private Long parentId;
    @Schema(description = "分类编码")
    private String categoryCode;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "排序值")
    private Integer sort;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @Schema(description = "子项列表")
    private List<CmsContentCategoryVO> children = new ArrayList<>();
}
