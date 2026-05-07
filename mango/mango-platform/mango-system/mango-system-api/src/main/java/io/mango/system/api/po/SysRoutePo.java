package io.mango.system.api.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "系统路由")
public class SysRoutePo {
    @Schema(description = "路由ID")
    private Long id;

    @Schema(description = "路由名称")
    @NotBlank(message = "routeName不能为空")
    private String routeName;

    @Schema(description = "路由类型")
    @NotNull(message = "routeType不能为空")
    private Integer routeType;

    @Schema(description = "路由路径")
    @NotBlank(message = "routePath不能为空")
    private String routePath;

    @Schema(description = "路由描述")
    private String routeDesc;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
