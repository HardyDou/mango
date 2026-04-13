package io.mango.system.api.po;

import io.mango.common.po.BasePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoutePo extends BasePO {
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
