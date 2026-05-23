package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发号历史分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "发号历史分页查询")
public class NumgenHistoryPageQuery extends PageQuery {

    @Schema(description = "编号规则键")
    private String genKey;

    @Schema(description = "编号结果，支持模糊搜索")
    private String resultNo;

    @Schema(description = "状态：1-成功，0-失败")
    private Integer status;

    @Schema(description = "规则版本")
    private Integer ruleVersion;

    @Schema(description = "业务键")
    private String bizKey;
}
