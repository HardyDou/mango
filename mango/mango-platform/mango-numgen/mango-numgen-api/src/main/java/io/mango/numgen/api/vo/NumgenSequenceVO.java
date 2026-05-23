package io.mango.numgen.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 编号序列视图。
 */
@Data
@Schema(description = "编号序列视图")
public class NumgenSequenceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号序列 ID")
    private Long id;

    @Schema(description = "编号规则键")
    private String genKey;

    @Schema(description = "规则版本")
    private Integer ruleVersion;

    @Schema(description = "当前序列值")
    private Long currentValue;

    @Schema(description = "版本")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
