package io.mango.system.api.vo;

import io.mango.system.api.enums.ConfigTypeEnum;
import lombok.Data;

@Data
public class SysConfigVO {
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private ConfigTypeEnum type;
    private Integer sort;
    private Integer status;
}
