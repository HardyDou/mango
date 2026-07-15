package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编号规则分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编号规则分页查询")
public class NumgenRulePageQuery extends PageQuery {

    @Size(max = 128, message = "规则键不能超过128个字符")
    @Schema(description = "规则键")
    private String genKey;

    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "关键词。支持规则键、规则名称模糊搜索")
    private String keyword;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Min(value = 0, message = "发布状态只能为0或1")
    @Max(value = 1, message = "发布状态只能为0或1")
    @Schema(description = "发布状态")
    private Integer publishStatus;
}
