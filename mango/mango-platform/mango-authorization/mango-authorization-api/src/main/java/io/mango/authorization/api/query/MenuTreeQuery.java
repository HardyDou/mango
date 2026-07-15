package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Size(max = 64)
    private String appCode;
    @Schema(description = "能力模块编码")
    @Size(max = 100)
    private String moduleCode;
    @Schema(description = "返回格式：list-列表，tree-树形；默认 list")
    @Pattern(regexp = "(?i)list|tree")
    private String fmt;
    @Schema(description = "菜单类型")
    @Min(1)
    @Max(3)
    private Integer type;
    @Schema(description = "父菜单ID")
    @Positive
    private Long parentId;
    @Schema(description = "菜单名称")
    @Size(max = 100)
    private String menuName;
    @Schema(description = "菜单状态：0-禁用，1-启用")
    @Min(0)
    @Max(1)
    private Integer status;
}
