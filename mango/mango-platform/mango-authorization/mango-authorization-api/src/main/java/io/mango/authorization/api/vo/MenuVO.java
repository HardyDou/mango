package io.mango.authorization.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单 VO。
 */
@Data
public class MenuVO {

    private static final long serialVersionUID = 1L;
    private Long menuId;
    private String appCode;
    private Long parentId;
    private Integer menuType;
    private String menuName;
    private String menuCode;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer status;
    private Integer visible;
    private Integer keepAlive;
    private Integer embedded;
    private String redirect;
    private String permissions;
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String remark;
    private MenuMeta meta;
    private List<MenuVO> children;

    /**
     * 菜单前端元信息。
     */
    @Data
    public static class MenuMeta {
        private String title;
        private String icon;
        private Boolean isAffix;
        private Boolean isLink;
        private String link;
        private Boolean isFrame;
        private String frameSrc;
        private String activeMenu;
        private Boolean breadcrumbHidden;
        private String[] permissions;
        private String badge;
        private String badgeType;
        private Boolean dot;
    }
}
