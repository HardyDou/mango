package io.mango.authorization.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 菜单授权套餐命令。
 */
@Data
public class MenuPackageCommand {

    private Long packageId;

    @NotBlank(message = "packageName不能为空")
    @Size(max = 100, message = "packageName长度不能超过100")
    private String packageName;

    @NotBlank(message = "packageCode不能为空")
    @Size(max = 64, message = "packageCode长度不能超过64")
    private String packageCode;

    @NotBlank(message = "appCode不能为空")
    @Size(max = 64, message = "appCode长度不能超过64")
    private String appCode;

    @NotNull(message = "status不能为空")
    private Integer status;

    private Integer sort;

    @Size(max = 500, message = "remark长度不能超过500")
    private String remark;

    @NotEmpty(message = "menuIds不能为空")
    private List<Long> menuIds;
}
