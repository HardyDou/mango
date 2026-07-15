package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用模块资源清单注册命令。
 */
@Data
@Schema(description = "应用模块资源清单注册命令")
public class AppModuleResourceManifestCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "逻辑应用编码")
    private String appCode;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "能力模块编码，来自 module.properties 的 module-name")
    private String moduleCode;

    @Schema(description = "能力模块名称")
    @Size(max = 100)
    private String moduleName;

    @Schema(description = "状态：0-停用，1-启用")
    @Min(0)
    @Max(1)
    private Integer status;

    @Schema(description = "排序号")
    @Min(0)
    private Integer sort;

    @Schema(description = "菜单同步到的套餐编码列表；为空时不自动加入套餐")
    @Size(max = 100)
    private List<String> packageCodes = new ArrayList<>();

    @Schema(description = "菜单默认授权到的角色编码列表；为空时不自动授权角色")
    @Size(max = 100)
    private List<String> roleCodes = new ArrayList<>();

    @Valid
    @Size(max = 1000)
    @Schema(description = "菜单树")
    private List<AppModuleMenuRequest> menus = new ArrayList<>();
}
