package io.mango.domain.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务域分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务域分页查询")
public class DomainPageQuery extends PageQuery {

    @Size(max = 64, message = "业务域编码长度不能超过64个字符")
    @Schema(description = "业务域编码")
    private String domainCode;

    @Size(max = 128, message = "业务域名称长度不能超过128个字符")
    @Schema(description = "业务域名称")
    private String domainName;

    @Min(value = 0, message = "业务域状态非法")
    @Max(value = 1, message = "业务域状态非法")
    @Schema(description = "状态：0停用，1启用")
    private Integer status;
}
