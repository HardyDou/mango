package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编号规则片段分页查询")
public class NumgenSegmentPageQuery extends PageQuery {

    @Positive(message = "规则 ID 必须大于0")
    @Schema(description = "规则 ID")
    private Long ruleId;
}
