package io.mango.resource.starter.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "资源日志分页查询")
public class ResourceLogPageQuery extends PageQuery {

    @Schema(description = "资源注册记录ID")
    private Long resourceId;
}
