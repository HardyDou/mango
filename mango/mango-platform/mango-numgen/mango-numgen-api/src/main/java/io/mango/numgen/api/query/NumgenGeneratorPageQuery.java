package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编号生成器分页查询")
public class NumgenGeneratorPageQuery extends PageQuery {

    @Schema(description = "关键词。支持业务 Key、名称模糊搜索")
    private String keyword;

    @Schema(description = "业务域编码")
    private String domainCode;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;
}
