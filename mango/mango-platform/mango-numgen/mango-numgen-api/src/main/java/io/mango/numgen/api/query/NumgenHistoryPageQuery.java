package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发号历史分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "发号历史分页查询")
public class NumgenHistoryPageQuery extends PageQuery {

    @Size(max = 128, message = "编号规则键不能超过128个字符")
    @Schema(description = "编号规则键")
    private String genKey;

    @Size(max = 256, message = "编号结果不能超过256个字符")
    @Schema(description = "编号结果，支持模糊搜索")
    private String resultNo;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态：1-成功，0-失败")
    private Integer status;

    @Positive(message = "规则版本必须大于0")
    @Schema(description = "规则版本")
    private Integer ruleVersion;

    @Size(max = 128, message = "业务键不能超过128个字符")
    @Schema(description = "业务键")
    private String bizKey;
}
