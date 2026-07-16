package io.mango.system.api.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "保存字典类型命令")
public class SaveDictTypeCommand {
    @Positive(message = "主键 ID必须大于 0")
    @Schema(description = "主键 ID")
    private Long id;
    @NotBlank(message = "dictType不能为空")
    @Size(max = 50, message = "dictType长度不能超过50")
    @Schema(description = "字典类型编码")
    private String dictType;
    @NotBlank(message = "dictName不能为空")
    @Size(max = 100, message = "dictName长度不能超过100")
    @Schema(description = "字典类型名称")
    private String dictName;
    @Size(max = 64, message = "domainCode长度不能超过64")
    @Schema(description = "业务域编码")
    private String domainCode;
    @Schema(description = "状态")
    @Max(value = Integer.MAX_VALUE, message = "状态不正确")
    private Integer status;
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不正确")
    private String remark;
}
