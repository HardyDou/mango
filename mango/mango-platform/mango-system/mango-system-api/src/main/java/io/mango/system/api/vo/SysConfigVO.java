package io.mango.system.api.vo;

import io.mango.system.api.enums.ConfigTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "系统配置视图")
public class SysConfigVO {
    @Schema(description = "配置ID")
    private Long id;
    @Schema(description = "配置键")
    private String configKey;
    @Schema(description = "配置值")
    private String configValue;
    @Schema(description = "配置名称")
    private String configName;
    @Schema(description = "配置类型")
    private ConfigTypeEnum type;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
