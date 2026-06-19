package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
    @Schema(description = "逻辑应用编码")
    private String appCode;

    @NotBlank
    @Schema(description = "能力模块编码，来自 module.properties 的 module-name")
    private String moduleCode;

    @Schema(description = "能力模块名称")
    private String moduleName;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "菜单同步到的套餐编码列表；为空时不自动加入套餐")
    private List<String> packageCodes = new ArrayList<>();

    @Schema(description = "菜单默认授权到的角色编码列表；为空时不自动授权角色")
    private List<String> roleCodes = new ArrayList<>();

    @Valid
    @Schema(description = "菜单树")
    private List<Menu> menus = new ArrayList<>();

    /**
     * 资源清单菜单节点。
     */
    @Data
    @Schema(description = "资源清单菜单节点")
    public static class Menu implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "菜单类型：1-目录，2-菜单，3-按钮")
        private Integer menuType;

        @NotBlank
        @Schema(description = "菜单名称")
        private String menuName;

        @NotBlank
        @Schema(description = "菜单编码或权限码")
        private String menuCode;

        @Schema(description = "父级菜单编码；为空时使用清单树父节点，根节点默认为一级菜单")
        private String parentCode;

        @Schema(description = "前端路由路径")
        private String path;

        @Schema(description = "页面运行类型：LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK/BUTTON")
        private String pageType;

        @Schema(description = "iframe 或外链地址")
        private String externalUrl;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "排序号")
        private Integer sort;

        @Schema(description = "状态：0-禁用，1-启用")
        private Integer status;

        @Schema(description = "是否显示：0-隐藏，1-显示")
        private Integer visible;

        @Schema(description = "前端组件路径")
        private String component;

        @Schema(description = "是否缓存：0-否，1-是")
        private Integer keepAlive;

        @Schema(description = "是否内嵌：0-否，1-是")
        private Integer embedded;

        @Schema(description = "重定向地址")
        private String redirect;

        @Schema(description = "页面携带的权限编码列表")
        private List<String> permissions = new ArrayList<>();

        @Schema(description = "当前菜单同步到的套餐编码列表；为空时继承父菜单或清单级套餐配置，空数组表示不绑定套餐")
        private List<String> packageCodes;

        @Schema(description = "当前菜单默认授权到的角色编码列表；为空时继承父菜单或清单级角色配置，空数组表示不授权角色")
        private List<String> roleCodes;

        @Valid
        @Schema(description = "页面下的按钮权限")
        private List<Permission> permissionItems = new ArrayList<>();

        @Valid
        @Schema(description = "子菜单")
        private List<Menu> children = new ArrayList<>();

        @Schema(description = "备注")
        private String remark;
    }

    /**
     * 资源清单按钮权限节点。
     */
    @Data
    @Schema(description = "资源清单按钮权限节点")
    public static class Permission implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "按钮菜单编码；为空时使用权限码")
        private String menuCode;

        @NotBlank
        @Schema(description = "权限码")
        private String permissionCode;

        @NotBlank
        @Schema(description = "权限名称")
        private String permissionName;

        @Schema(description = "排序号")
        private Integer sort;

        @Schema(description = "状态：0-禁用，1-启用")
        private Integer status;

        @Schema(description = "当前按钮同步到的套餐编码列表；为空时继承所属菜单套餐配置，空数组表示不绑定套餐")
        private List<String> packageCodes;

        @Schema(description = "当前按钮默认授权到的角色编码列表；为空时继承所属菜单角色配置，空数组表示不授权角色")
        private List<String> roleCodes;

        @Schema(description = "备注")
        private String remark;
    }
}
