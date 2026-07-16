package io.mango.org.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织信息。
 */
@Data
@Schema(description = "组织信息")
public class SysOrgVO {

    @Schema(description = "组织ID")
    private Long id;

    @Schema(description = "父级组织ID，根节点为0")
    private Long pid;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "组织编码")
    private String orgCode;

    @Schema(description = "组织类型：1-集团，2-公司，3-部门，4-小组")
    private Integer orgType;

    @Schema(description = "排序值")
    private Integer orgSort;

    @Schema(description = "组织状态：0-禁用，1-启用")
    private String orgStatus;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "子组织列表")
    private List<SysOrgVO> children = new ArrayList<>();
}
