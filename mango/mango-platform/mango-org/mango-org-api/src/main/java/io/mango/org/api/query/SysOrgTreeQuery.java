package io.mango.org.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private Long parentId;

    @Schema(description = "组织类型：1-集团，2-公司，3-部门，4-小组")
    private Integer type;
}
