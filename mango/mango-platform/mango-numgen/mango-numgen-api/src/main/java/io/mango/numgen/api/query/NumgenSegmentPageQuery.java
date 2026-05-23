package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编号规则片段分页查询")
public class NumgenSegmentPageQuery extends PageQuery {

    @Schema(description = "规则 ID")
    private Long ruleId;
}
