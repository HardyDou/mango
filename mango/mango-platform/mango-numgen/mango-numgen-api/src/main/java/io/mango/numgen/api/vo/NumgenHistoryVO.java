package io.mango.numgen.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发号历史视图。
 */
@Data
@Schema(description = "发号历史视图")
public class NumgenHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "发号历史 ID")
    private Long id;

    @Schema(description = "编号规则键")
    private String genKey;

    @Schema(description = "规则 ID")
    private Long ruleId;

    @Schema(description = "编号结果")
    private String resultNo;

    @Schema(description = "规则版本")
    private Integer ruleVersion;

    @Schema(description = "业务键")
    private String bizKey;

    @Schema(description = "输入摘要")
    private String inputDigest;

    @Schema(description = "耗时毫秒")
    private Long costMillis;

    @Schema(description = "状态：1-成功，0-失败")
    private Integer status;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
