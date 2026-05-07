package io.mango.system.api.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "字典数据")
public class DictDataPo {
    @Schema(description = "字典数据ID")
    private Long id;

    @Schema(description = "字典标签")
    @NotBlank(message = "dictLabel不能为空")
    @Size(max = 100, message = "dictLabel长度不能超过100")
    private String dictLabel;

    @Schema(description = "字典值")
    @NotBlank(message = "dictValue不能为空")
    @Size(max = 100, message = "dictValue长度不能超过100")
    private String dictValue;

    @Schema(description = "字典类型编码")
    @NotBlank(message = "dictType不能为空")
    private String dictType;

    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
