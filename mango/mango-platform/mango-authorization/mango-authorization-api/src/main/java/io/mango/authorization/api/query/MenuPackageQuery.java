package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 菜单授权套餐查询条件。 */
@Data
@Schema(description = "菜单授权套餐查询条件")
public class MenuPackageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 100)
    @Schema(description = "应用编码")
    private String appCode;

    @Size(max = 100)
    @Schema(description = "套餐名称或编码关键字")
    private String keyword;

    @Min(0)
    @Max(1)
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
}
