package io.mango.system.api.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "保存字典数据命令")
public class SaveDictDataCommand {
    @Positive(message = "主键 ID必须大于 0")
    @Schema(description = "主键 ID")
    private Long id;
    @NotBlank(message = "dictLabel不能为空")
    @Size(max = 100, message = "dictLabel长度不能超过100")
    @Schema(description = "字典标签")
    private String dictLabel;
    @NotBlank(message = "dictValue不能为空")
    @Size(max = 100, message = "dictValue长度不能超过100")
    @Schema(description = "字典值")
    private String dictValue;
    @NotBlank(message = "dictType不能为空")
    @Size(max = 50, message = "dictType长度不能超过50")
    @Schema(description = "字典类型编码")
    private String dictType;
    @Schema(description = "排序号")
    @PositiveOrZero(message = "排序号不能小于 0")
    private Integer sort;
    @Schema(description = "状态")
    @Max(value = Integer.MAX_VALUE, message = "状态不正确")
    private Integer status;
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不正确")
    private String remark;
}
