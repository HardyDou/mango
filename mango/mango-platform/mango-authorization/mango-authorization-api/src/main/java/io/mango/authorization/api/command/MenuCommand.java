package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单创建或修改命令。
 */
@Data
@Schema(description = "菜单创建或修改命令")
public class MenuCommand implements Serializable {

    private static final long serialVersionUID = 1L;
    @Schema(description = "菜单ID，创建时为空，修改时必填")
    private Long menuId;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "能力模块编码，来自 module.properties 的 module-name")
    private String moduleCode;
    @Schema(description = "父菜单ID")
    private Long parentId;
    @Schema(description = "菜单类型")
    private Integer menuType;
    @Schema(description = "菜单名称")
    private String menuName;
    @Schema(description = "菜单编码")
    private String menuCode;
    @Schema(description = "路由路径")
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
    @Schema(description = "权限编码")
    private String permissions;
    @Schema(description = "按钮类型：TABLE-表格按钮，NON_TABLE-非表格按钮")
    private String buttonType;
    @Schema(description = "按钮展示规则表达式")
    private String buttonDisplayRule;
    @Schema(description = "创建人")
    private String createBy;
    @Schema(description = "更新人")
    private String updateBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "删除标记：0-正常，1-删除")
    private Integer delFlag;
}
