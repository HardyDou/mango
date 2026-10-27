package io.mango.system.api.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "树形字典节点")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Tree VO exposes its children list for JSON serialization and tree assembly")
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
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "子节点")
    private List<DictTreeNodeVO> children = new ArrayList<>();
}
