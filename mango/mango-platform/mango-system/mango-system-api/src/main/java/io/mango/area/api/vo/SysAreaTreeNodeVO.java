package io.mango.area.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "行政区划树节点")
public class SysAreaTreeNodeVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "父级 ID")
    private Long pid;
    @Schema(description = "父级 ID")
    private Long parentId;
    @Schema(description = "行政区划编码")
    private Long adcode;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "层级")
    private Integer level;
    @Schema(description = "是否热门")
    private String hot;
    @Schema(description = "子节点")
    private List<SysAreaTreeNodeVO> children = new ArrayList<>();
    @Schema(description = "是否叶子节点")
    private boolean leaf;

    public List<SysAreaTreeNodeVO> getChildren() {
        return List.copyOf(children);
    }

    public void setChildren(List<SysAreaTreeNodeVO> children) {
        if (children == null) {
            this.children = new ArrayList<>();
            return;
        }
        this.children = new ArrayList<>(children);
    }
}
