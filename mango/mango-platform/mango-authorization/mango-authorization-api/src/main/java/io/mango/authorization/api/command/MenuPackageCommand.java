package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/**
 * 菜单授权套餐命令。
 */
@Data
@Schema(description = "菜单授权套餐命令")
public class MenuPackageCommand {

    @Positive
    @Schema(description = "套餐ID，修改时必填")
    private Long packageId;

    @NotBlank(message = "packageName不能为空")
    @Size(max = 100, message = "packageName长度不能超过100")
    @Schema(description = "套餐名称")
    private String packageName;

    @NotBlank(message = "packageCode不能为空")
    @Size(max = 64, message = "packageCode长度不能超过64")
    @Schema(description = "套餐编码")
    private String packageCode;

    @NotBlank(message = "appCode不能为空")
    @Size(max = 64, message = "appCode长度不能超过64")
    @Schema(description = "应用编码")
    private String appCode;

    @NotNull(message = "status不能为空")
    @Min(0)
    @Max(1)
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Min(0)
    @Schema(description = "排序号")
    private Integer sort;

    @Size(max = 500, message = "remark长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    @NotEmpty(message = "menuIds不能为空")
    @Schema(description = "菜单ID列表")
    private List<@Positive Long> menuIds;
}
