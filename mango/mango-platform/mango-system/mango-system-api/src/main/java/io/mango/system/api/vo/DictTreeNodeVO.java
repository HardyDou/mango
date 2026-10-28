package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "树形字典节点")
public class DictTreeNodeVO {
    @Schema(description = "字典数据 ID")
    private Long id;
    @Schema(description = "字典类型编码")
    private String dictType;
    @Schema(description = "字典标签")
    private String dictLabel;
    @Schema(description = "字典值")
    private String dictValue;
    @Schema(description = "父节点值，空值表示根节点")
    private String parentValue;
    @Schema(description = "分组标记，空列表表示未分组")
    private List<String> groups;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "子节点")
    private final List<DictTreeNodeVO> children = new ArrayList<>();

    /**
     * 返回子节点快照，避免外部直接持有可变集合。
     */
    public List<DictTreeNodeVO> getChildren() {
        return List.copyOf(children);
    }

    public void setChildren(List<DictTreeNodeVO> children) {
        this.children.clear();
        if (children != null) {
            this.children.addAll(children);
        }
    }

    /**
     * 追加子节点，供树装配时使用。
     */
    public void addChild(DictTreeNodeVO child) {
        this.children.add(child);
    }
}
