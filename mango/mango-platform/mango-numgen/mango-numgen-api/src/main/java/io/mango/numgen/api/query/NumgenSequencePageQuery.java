package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编号序列分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编号序列分页查询")
public class NumgenSequencePageQuery extends PageQuery {

    @Schema(description = "编号规则键")
    private String genKey;

    @Schema(description = "规则版本")
    private Integer ruleVersion;
}
