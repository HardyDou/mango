package io.mango.numgen.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编号生成器分页查询")
public class NumgenGeneratorPageQuery extends PageQuery {

    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "关键词。支持业务 Key、名称模糊搜索")
    private String keyword;

    @Size(max = 64, message = "业务域编码不能超过64个字符")
    @Schema(description = "业务域编码")
    private String domainCode;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;
}
