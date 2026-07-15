package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "菜单前端元信息")
public class MenuMetaVO {

    @Schema(description = "标题")
    private String title;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "是否固定页签")
    private Boolean isAffix;
    @Schema(description = "是否外部链接")
    private Boolean isLink;
    @Schema(description = "外部链接地址")
    private String link;
    @Schema(description = "是否 iframe")
    private Boolean isFrame;
    @Schema(description = "iframe 地址")
    private String frameSrc;
    @Schema(description = "激活菜单路径")
    private String activeMenu;
    @Schema(description = "是否隐藏面包屑")
    private Boolean breadcrumbHidden;
    @Schema(description = "权限码集合")
    private String[] permissions;
    @Schema(description = "徽标内容")
    private String badge;
    @Schema(description = "徽标类型")
    private String badgeType;
    @Schema(description = "是否显示红点")
    private Boolean dot;
}
