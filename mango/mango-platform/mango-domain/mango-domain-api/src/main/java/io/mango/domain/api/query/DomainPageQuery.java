package io.mango.domain.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务域分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务域分页查询")
public class DomainPageQuery extends PageQuery {

    @Schema(description = "业务域编码")
    private String domainCode;

    @Schema(description = "业务域名称")
    private String domainName;

    @Schema(description = "状态：0停用，1启用")
    private Integer status;
}
