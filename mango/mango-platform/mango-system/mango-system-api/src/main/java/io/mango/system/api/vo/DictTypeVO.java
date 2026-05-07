package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "字典类型视图")
public class DictTypeVO {
    @Schema(description = "字典类型ID")
    private Long id;
    @Schema(description = "字典类型编码")
    private String dictType;
    @Schema(description = "字典类型名称")
    private String dictName;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
