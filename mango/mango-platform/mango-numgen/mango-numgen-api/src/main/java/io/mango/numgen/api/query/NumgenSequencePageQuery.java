package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编号序列分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编号序列分页查询")
public class NumgenSequencePageQuery extends PageQuery {

    @Size(max = 128, message = "编号规则键不能超过128个字符")
    @Schema(description = "编号规则键")
    private String genKey;

    @Positive(message = "规则版本必须大于0")
    @Schema(description = "规则版本")
    private Integer ruleVersion;

    @Size(max = 256, message = "流水分组键不能超过256个字符")
    @Schema(description = "流水分组键")
    private String scopeKey;
}
