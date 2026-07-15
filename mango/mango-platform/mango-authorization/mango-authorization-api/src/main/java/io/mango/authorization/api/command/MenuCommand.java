package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
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
    @Positive
    private Long menuId;
    @Schema(description = "应用编码")
    @NotBlank
    @Size(max = 64)
    private String appCode;
    @Schema(description = "能力模块编码，来自 module.properties 的 module-name")
    @NotBlank
    @Size(max = 100)
    private String moduleCode;
    @Schema(description = "父菜单ID")
    @PositiveOrZero
    private Long parentId;
    @Schema(description = "菜单类型")
    @NotNull
    @Min(1)
    @Max(3)
    private Integer menuType;
    @Schema(description = "菜单名称")
    @NotBlank
    @Size(max = 100)
    private String menuName;
    @Schema(description = "菜单编码")
    @NotBlank
    @Size(max = 100)
    private String menuCode;
    @Schema(description = "路由路径")
    @Size(max = 500)
    private String path;
    @Schema(description = "页面运行类型：LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK/BUTTON")
    @Size(max = 32)
    private String pageType;
    @Schema(description = "iframe 或外链地址")
    @Size(max = 1000)
    private String externalUrl;
    @Schema(description = "图标")
    @Size(max = 100)
    private String icon;
    @Schema(description = "排序号")
    @Min(0)
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    @Min(0)
    @Max(1)
    private Integer status;
    @Schema(description = "是否显示：0-隐藏，1-显示")
    @Min(0)
    @Max(1)
    private Integer visible;
    @Schema(description = "前端组件路径")
    @Size(max = 500)
    private String component;
    @Schema(description = "是否缓存：0-否，1-是")
    @Min(0)
    @Max(1)
    private Integer keepAlive;
    @Schema(description = "是否内嵌：0-否，1-是")
    @Min(0)
    @Max(1)
    private Integer embedded;
    @Schema(description = "重定向地址")
    @Size(max = 500)
    private String redirect;
    @Schema(description = "历史权限编码，运行鉴权不再读取")
    @Size(max = 1000)
    private String permissions;
    @Schema(description = "菜单携带的接口/动作权限码列表，逗号分隔")
    @Size(max = 2000)
    private String apiCodes;
    @Schema(description = "按钮类型：TABLE-表格按钮，NON_TABLE-非表格按钮")
    @Size(max = 32)
    private String buttonType;
    @Schema(description = "按钮展示规则表达式")
    @Size(max = 1000)
    private String buttonDisplayRule;
    @Schema(description = "创建人")
    @Size(max = 64)
    private String createBy;
    @Schema(description = "更新人")
    @Size(max = 64)
    private String updateBy;
    @Schema(description = "创建时间")
    @PastOrPresent
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    @PastOrPresent
    private LocalDateTime updateTime;
    @Schema(description = "备注")
    @Size(max = 500)
    private String remark;
    @Schema(description = "删除标记：0-正常，1-删除")
    @Min(0)
    @Max(1)
    private Integer delFlag;
}
