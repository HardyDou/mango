package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
