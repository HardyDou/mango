package io.mango.system.api.po;

import io.mango.common.po.BasePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypePo extends BasePO {
    private Long id;

    @NotBlank(message = "dictType不能为空")
    @Size(max = 50, message = "dictType长度不能超过50")
    private String dictType;

    @NotBlank(message = "dictName不能为空")
    @Size(max = 100, message = "dictName长度不能超过100")
    private String dictName;

    private Integer status;
    private String remark;
}
