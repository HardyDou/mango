package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编号规则分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编号规则分页查询")
public class NumgenRulePageQuery extends PageQuery {

    @Schema(description = "规则键")
    private String genKey;

    @Schema(description = "关键词。支持规则键、规则名称模糊搜索")
    private String keyword;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "发布状态")
    private Integer publishStatus;
}
