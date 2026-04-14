package io.mango.system.api.po;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysRoutePo {
    private Long id;

    @NotBlank(message = "routeName不能为空")
    private String routeName;

    @NotNull(message = "routeType不能为空")
    private Integer routeType;

    @NotBlank(message = "routePath不能为空")
    private String routePath;

    private String routeDesc;
    private Integer sort;
    private Integer status;
}
