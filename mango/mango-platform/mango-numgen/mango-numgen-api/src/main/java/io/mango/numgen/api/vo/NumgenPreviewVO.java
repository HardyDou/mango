package io.mango.numgen.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "编号预览视图")
public class NumgenPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号规则键")
    private String genKey;

    @Schema(description = "规则版本")
    private Integer ruleVersion;

    @Schema(description = "片段列表")
    private List<NumgenPreviewSegmentVO> segments = new ArrayList<>();

    @Schema(description = "预览编号")
    private List<String> values = new ArrayList<>();
}
