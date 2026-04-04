package io.mango.system.api.po;

import io.mango.common.po.BasePO;
import io.mango.system.api.enums.ConfigTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigPo extends BasePO {
    private Long id;

    @NotBlank(message = "configKey不能为空")
    @Size(max = 100, message = "configKey长度不能超过100")
    private String configKey;

    @NotBlank(message = "configValue不能为空")
    private String configValue;

    @NotBlank(message = "configName不能为空")
    @Size(max = 100, message = "configName长度不能超过100")
    private String configName;

    @NotNull(message = "type不能为空")
    private ConfigTypeEnum type;

    private Integer sort;
    private Integer status;
    private String remark;
}
