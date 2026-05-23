package io.mango.numgen.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "编号规则片段视图")
public class NumgenRuleSegmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "片段 ID")
    private Long id;

    @Schema(description = "规则 ID")
    private Long ruleId;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "片段类型")
    private String segmentType;

    @Schema(description = "片段名称")
    private String segmentName;

    @Schema(description = "字符串内容，支持 ${参数ID} 占位符")
    private String literalValue;

    @Schema(description = "变量键")
    private String variableKey;

    @Schema(description = "日期格式")
    private String dateFormat;

    @Schema(description = "流水位数")
    private Integer seqWidth;

    @Schema(description = "补齐字符")
    private String padChar;

    @Schema(description = "是否参与流水分组：0-否，1-是")
    private Integer sequenceScope;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
