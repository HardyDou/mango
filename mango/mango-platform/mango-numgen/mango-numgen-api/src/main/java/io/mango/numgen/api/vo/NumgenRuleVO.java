package io.mango.numgen.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 编号规则视图。
 */
@Data
@Schema(description = "编号规则视图")
public class NumgenRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号规则 ID")
    private Long id;

    @Schema(description = "编号规则键")
    private String genKey;

    @Schema(description = "业务名称")
    private String genName;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则版本")
    private Integer version;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "发布状态")
    private Integer publishStatus;

    @Schema(description = "版本状态：DRAFT-草稿，ACTIVE-生效中，HISTORY-历史")
    private String versionState;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
