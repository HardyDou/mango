package io.mango.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_config")
public class SysConfigEntity extends TenantEntity {
    private String configKey;
    private String configValue;
    private String configName;
    private ConfigTypeEnum type;
    private String domainCode;
    private ConfigValueTypeEnum valueType;
    private String groupCode;
    private String groupName;
    private String defaultValue;
    private String options;
    private ConfigOptionSourceEnum optionSource;
    private String dictType;
    private Boolean editable;
    private String editableReason;
    private Integer sort;
    private Integer status;
    private String remark;
}
