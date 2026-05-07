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
    @Schema(description = "菜单类型")
    private Integer type;
    @Schema(description = "父菜单ID")
    private Long parentId;
    @Schema(description = "菜单名称")
    private String menuName;
}
