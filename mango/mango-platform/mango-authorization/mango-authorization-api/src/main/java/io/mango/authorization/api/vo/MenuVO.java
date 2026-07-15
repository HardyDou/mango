package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单 VO。
 */
@Data
@Schema(description = "菜单信息")
public class MenuVO {

    private static final long serialVersionUID = 1L;
    @Schema(description = "菜单ID")
    private Long menuId;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "模块编码")
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
    @Schema(description = "页面运行类型")
    private String pageType;
    @Schema(description = "组件路径")
    private String component;
    @Schema(description = "外链地址")
    private String externalUrl;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "是否可见")
    private Integer visible;
    @Schema(description = "是否缓存")
    private Integer keepAlive;
    @Schema(description = "是否内嵌")
    private Integer embedded;
    @Schema(description = "重定向地址")
    private String redirect;
    @Schema(description = "历史权限编码")
    private String permissions;
    @Schema(description = "接口/动作权限码")
    private String apiCodes;
    @Schema(description = "按钮类型")
    private String buttonType;
    @Schema(description = "按钮展示规则")
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
    @Schema(description = "前端元信息")
    private MenuMetaVO meta;
    @Schema(description = "子菜单")
    private List<MenuVO> children;
}
