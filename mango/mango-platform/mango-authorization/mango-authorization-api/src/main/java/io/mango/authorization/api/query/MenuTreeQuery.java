package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单树查询条件。
 */
@Data
@Schema(description = "菜单树查询条件")
public class MenuTreeQuery implements Serializable {

    private static final long serialVersionUID = 1L;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "返回格式：list-列表，tree-树形；默认 list")
    private String fmt;
    @Schema(description = "菜单类型")
    private Integer type;
    @Schema(description = "父菜单ID")
    private Long parentId;
    @Schema(description = "菜单名称")
    private String menuName;
    @Schema(description = "菜单状态：0-禁用，1-启用")
    private Integer status;
}
