package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "应用模块菜单资源请求")
public class AppModuleMenuRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(1)
    @Max(3)
    @Schema(description = "菜单类型：1-目录，2-菜单，3-按钮")
    private Integer menuType;
    @NotBlank
    @Size(max = 100)
    @Schema(description = "菜单名称")
    private String menuName;
    @NotBlank
    @Size(max = 100)
    @Schema(description = "菜单编码")
    private String menuCode;
    @Size(max = 100)
    @Schema(description = "父级菜单编码")
    private String parentCode;
    @Size(max = 500)
    @Schema(description = "前端路由路径")
    private String path;
    @Size(max = 32)
    @Schema(description = "页面运行类型")
    private String pageType;
    @Size(max = 1000)
    @Schema(description = "iframe 或外链地址")
    private String externalUrl;
    @Size(max = 100)
    @Schema(description = "图标")
    private String icon;
    @Min(0)
    @Schema(description = "排序号")
    private Integer sort;
    @Min(0)
    @Max(1)
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Min(0)
    @Max(1)
    @Schema(description = "是否显示：0-隐藏，1-显示")
    private Integer visible;
    @Size(max = 500)
    @Schema(description = "前端组件路径")
    private String component;
    @Min(0)
    @Max(1)
    @Schema(description = "是否缓存：0-否，1-是")
    private Integer keepAlive;
    @Min(0)
    @Max(1)
    @Schema(description = "是否内嵌：0-否，1-是")
    private Integer embedded;
    @Size(max = 500)
    @Schema(description = "重定向地址")
    private String redirect;
    @Size(max = 200)
    @Schema(description = "历史页面权限编码列表")
    private List<@Size(max = 100) String> permissions = new ArrayList<>();
    @Size(max = 200)
    @Schema(description = "接口/动作权限码列表")
    private List<@Size(max = 100) String> apiCodes = new ArrayList<>();
    @Size(max = 100)
    @Schema(description = "套餐编码列表")
    private List<@Size(max = 64) String> packageCodes;
    @Size(max = 100)
    @Schema(description = "默认角色编码列表")
    private List<@Size(max = 50) String> roleCodes;
    @Valid
    @Size(max = 200)
    @Schema(description = "历史按钮权限节点")
    private List<AppModulePermissionRequest> permissionItems = new ArrayList<>();
    @Valid
    @Size(max = 500)
    @Schema(description = "子菜单")
    private List<AppModuleMenuRequest> children = new ArrayList<>();
    @Size(max = 500)
    @Schema(description = "备注")
    private String remark;
}
