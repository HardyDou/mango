package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * System menu VO
 *
 * @author Mango
 */
@Data
@Schema(description = "菜单VO")
public class SysMenuVO {

    private static final long serialVersionUID = 1L;

    /**
     * Menu ID
     */
    @Schema(description = "菜单ID")
    private Long menuId;

    /**
     * Menu group ID
     */
    @Schema(description = "菜单分组ID")
    private Long groupId;

    /**
     * Parent menu ID (0: root)
     */
    @Schema(description = "父菜单ID")
    private Long parentId;

    /**
     * Menu type (1: directory, 2: menu, 3: button)
     */
    @Schema(description = "菜单类型: 1-目录, 2-菜单, 3-按钮")
    private Integer menuType;

    /**
     * Menu name
     */
    @Schema(description = "菜单名称")
    private String menuName;

    /**
     * Menu code/permission (e.g., system:user:view)
     */
    @Schema(description = "菜单权限标识")
    private String menuCode;

    /**
     * Path
     */
    @Schema(description = "前端路由路径")
    private String path;

    /**
     * Frontend component path
     */
    @Schema(description = "前端组件路径")
    private String component;

    /**
     * Icon
     */
    @Schema(description = "菜单图标")
    private String icon;

    /**
     * Sort order
     */
    @Schema(description = "排序号")
    private Integer sort;

    /**
     * Status (0: disabled, 1: enabled)
     */
    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    /**
     * Visible (0: hidden, 1: visible)
     */
    @Schema(description = "是否显示: 0-隐藏, 1-显示")
    private Integer visible;

    /**
     * Route cache (0: no cache, 1: cache)
     */
    @Schema(description = "路由缓存: 0-不缓存, 1-缓存")
    private Integer keepAlive;

    /**
     * Embedded mode (0: no, 1: iframe embed)
     */
    @Schema(description = "内嵌模式: 0-否, 1-iframe内嵌")
    private Integer embedded;

    /**
     * Redirect path
     */
    @Schema(description = "重定向路径")
    private String redirect;

    /**
     * Permission identifiers
     */
    @Schema(description = "权限标识列表")
    private String permissions;

    /**
     * Creator
     */
    @Schema(description = "创建人")
    private String createBy;

    /**
     * Updater
     */
    @Schema(description = "修改人")
    private String updateBy;

    /**
     * Create time
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * Update time
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * Remark
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * Meta info for frontend rendering
     */
    @Schema(description = "前端Meta信息")
    private MenuMeta meta;

    /**
     * Child menus
     */
    @Schema(description = "子菜单")
    private List<SysMenuVO> children;

    /**
     * Menu meta info for frontend
     */
    @Data
    @Schema(description = "菜单Meta信息")
    public static class MenuMeta {
        /**
         * Menu title
         */
        @Schema(description = "菜单标题")
        private String title;

        /**
         * Menu icon
         */
        @Schema(description = "菜单图标")
        private String icon;

        /**
         * Is affix (fixed in tab bar)
         */
        @Schema(description = "是否固定在标签栏")
        private Boolean isAffix;

        /**
         * Is link (external link)
         */
        @Schema(description = "是否外部链接")
        private Boolean isLink;

        /**
         * External link URL
         */
        @Schema(description = "外部链接地址")
        private String link;

        /**
         * Is frame (iframe embed)
         */
        @Schema(description = "是否内嵌iframe")
        private Boolean isFrame;

        /**
         * Iframe src (for embedded pages)
         */
        @Schema(description = "内嵌iframe地址")
        private String frameSrc;

        /**
         * Active menu path (highlight current menu)
         */
        @Schema(description = "高亮菜单路径")
        private String activeMenu;

        /**
         * Is show in breadcrumb
         */
        @Schema(description = "是否显示在面包屑")
        private Boolean breadcrumbHidden;

        /**
         * Required permissions
         */
        @Schema(description = "所需权限列表")
        private String[] permissions;

        /**
         * Badge text
         */
        @Schema(description = "徽章文字")
        private String badge;

        /**
         * Badge type (primary/success/warning/danger/info)
         */
        @Schema(description = "徽章类型")
        private String badgeType;

        /**
         * Is show dot
         */
        @Schema(description = "是否显示点标")
        private Boolean dot;
    }
}
