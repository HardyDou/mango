package io.mango.system.api.po;

import io.mango.common.po.BasePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictDataPo extends BasePO {
    private Long id;

    @NotBlank(message = "dictLabel不能为空")
    @Size(max = 100, message = "dictLabel长度不能超过100")
    private String dictLabel;

    @NotBlank(message = "dictValue不能为空")
    @Size(max = 100, message = "dictValue长度不能超过100")
    private String dictValue;

    @NotBlank(message = "dictType不能为空")
    private String dictType;

    private Integer sort;
    private Integer status;
}
