package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资源处理器字段说明。
 */
@Data
@Schema(description = "资源处理器字段说明")
public class ResourceHandlerFieldVO {

    @Schema(description = "字段名")
    private String name;

    @Schema(description = "字段说明")
    private String description;
}
