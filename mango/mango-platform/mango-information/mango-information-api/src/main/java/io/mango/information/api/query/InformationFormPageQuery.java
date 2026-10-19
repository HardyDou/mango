package io.mango.information.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 表单分页查询。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InformationFormPageQuery extends PageQuery {
    @Size(max = 128, message = "关键词不能超过128个字符")
    @Schema(description = "匹配表单编码或名称")
    private String keyword;
    @Size(max = 32, message = "状态不能超过32个字符")
    private String status;
}
