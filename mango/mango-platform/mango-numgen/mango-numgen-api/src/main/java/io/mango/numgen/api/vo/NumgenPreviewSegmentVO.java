package io.mango.numgen.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "编号预览片段视图")
public class NumgenPreviewSegmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "片段类型")
    private String segmentType;

    @Schema(description = "片段名称")
    private String segmentName;

    @Schema(description = "渲染值")
    private String value;
}
