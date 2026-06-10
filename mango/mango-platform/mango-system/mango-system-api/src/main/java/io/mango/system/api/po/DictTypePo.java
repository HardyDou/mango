package io.mango.system.api.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "字典类型")
public class DictTypePo {
    @Schema(description = "字典类型ID")
    private Long id;

    @Schema(description = "字典类型编码")
    @NotBlank(message = "dictType不能为空")
    @Size(max = 50, message = "dictType长度不能超过50")
    private String dictType;

    @Schema(description = "字典类型名称")
    @NotBlank(message = "dictName不能为空")
    @Size(max = 100, message = "dictName长度不能超过100")
    private String dictName;

    @Schema(description = "业务域编码")
    @Size(max = 64, message = "domainCode长度不能超过64")
    private String domainCode;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
