package io.mango.system.api.po;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictTypePo {
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
