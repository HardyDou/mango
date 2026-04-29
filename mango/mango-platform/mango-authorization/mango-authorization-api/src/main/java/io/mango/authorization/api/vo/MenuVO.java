package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单 VO。
 */
@Data
@Schema(description = "菜单VO")
public class MenuVO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单类型: 1-目录, 2-菜单, 3-按钮")
    private Integer menuType;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单权限标识")
    private String menuCode;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "是否显示: 0-隐藏, 1-显示")
    private Integer visible;

    @Schema(description = "路由缓存: 0-不缓存, 1-缓存")
    private Integer keepAlive;

    @Schema(description = "内嵌模式: 0-否, 1-iframe内嵌")
    private Integer embedded;

    @Schema(description = "重定向路径")
    private String redirect;

    @Schema(description = "权限标识列表")
    private String permissions;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "修改人")
    private String updateBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "前端Meta信息")
    private MenuMeta meta;

    @Schema(description = "子菜单")
    private List<MenuVO> children;

    /**
     * 菜单前端元信息。
     */
    @Data
    @Schema(description = "菜单Meta信息")
    public static class MenuMeta {
        @Schema(description = "菜单标题")
        private String title;

        @Schema(description = "菜单图标")
        private String icon;

        @Schema(description = "是否固定在标签栏")
        private Boolean isAffix;

        @Schema(description = "是否外部链接")
        private Boolean isLink;

        @Schema(description = "外部链接地址")
        private String link;

        @Schema(description = "是否内嵌iframe")
        private Boolean isFrame;

        @Schema(description = "内嵌iframe地址")
        private String frameSrc;

        @Schema(description = "高亮菜单路径")
        private String activeMenu;

        @Schema(description = "是否显示在面包屑")
        private Boolean breadcrumbHidden;

        @Schema(description = "所需权限列表")
        private String[] permissions;

        @Schema(description = "徽章文字")
        private String badge;

        @Schema(description = "徽章类型")
        private String badgeType;

        @Schema(description = "是否显示点标")
        private Boolean dot;
    }
}
