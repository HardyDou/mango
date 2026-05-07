package io.mango.system.api.po;

import io.mango.system.api.enums.ConfigTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "系统配置")
public class SysConfigPo {
    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "配置键")
    @NotBlank(message = "configKey不能为空")
    @Size(max = 100, message = "configKey长度不能超过100")
    private String configKey;

    @Schema(description = "配置值")
    @NotBlank(message = "configValue不能为空")
    private String configValue;

    @Schema(description = "配置名称")
    @NotBlank(message = "configName不能为空")
    @Size(max = 100, message = "configName长度不能超过100")
    private String configName;

    @Schema(description = "配置类型")
    @NotNull(message = "type不能为空")
    private ConfigTypeEnum type;

    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
