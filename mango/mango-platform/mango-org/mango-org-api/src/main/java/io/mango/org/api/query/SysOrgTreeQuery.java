package io.mango.org.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.io.Serializable;

/**
 * 组织树查询条件。
 */
@Data
@Schema(description = "组织树查询条件")
public class SysOrgTreeQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "父级组织ID，根节点为 0")
    @PositiveOrZero(message = "父级组织ID不能小于0")
    private Long parentId;

    @Schema(description = "组织类型：1-集团，2-公司，3-部门，4-小组")
    @Min(value = 1, message = "组织类型不能小于1")
    @Max(value = 4, message = "组织类型不能大于4")
    private Integer type;

    @Schema(description = "是否包含禁用组织，默认 false")
    @NotNull(message = "是否包含禁用组织不能为空")
    private Boolean includeDisabled = Boolean.FALSE;
}
