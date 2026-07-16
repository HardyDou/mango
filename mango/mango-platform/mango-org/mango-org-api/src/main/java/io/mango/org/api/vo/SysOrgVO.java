package io.mango.org.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
    @EqualsAndHashCode.Exclude
    private List<SysOrgVO> children;

    public List<SysOrgVO> getChildren() {
        if (children == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(children);
    }

    public void setChildren(List<SysOrgVO> children) {
        if (children == null) {
            this.children = new ArrayList<>();
            return;
        }
        this.children = new ArrayList<>(children);
    }
}
