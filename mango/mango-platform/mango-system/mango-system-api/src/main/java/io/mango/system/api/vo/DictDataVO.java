package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "字典数据视图")
public class DictDataVO {
    @Schema(description = "字典数据ID")
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
    private List<String> groups = new ArrayList<>();
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;

    /**
     * 返回分组快照，避免直接暴露可变集合。
     */
    public List<String> getGroups() {
        return List.copyOf(groups);
    }

    public void setGroups(List<String> groups) {
        this.groups = groups == null ? new ArrayList<>() : new ArrayList<>(groups);
    }
}
