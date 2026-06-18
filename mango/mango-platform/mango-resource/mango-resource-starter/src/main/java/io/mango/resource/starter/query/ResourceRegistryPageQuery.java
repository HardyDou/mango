package io.mango.resource.starter.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "资源注册分页查询")
public class ResourceRegistryPageQuery extends PageQuery {

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "来源模块")
    private String moduleCode;

    @Schema(description = "目标模块")
    private String targetModule;

    @Schema(description = "同步模式")
    private String syncMode;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "关键词，支持资源ID、BizKey、名称")
    private String keyword;
}
