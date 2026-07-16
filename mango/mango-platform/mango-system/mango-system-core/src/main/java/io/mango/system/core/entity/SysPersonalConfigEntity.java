package io.mango.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_personal_config")
public class SysPersonalConfigEntity extends TenantEntity {
    private Long userId;
    private String groupCode;
    private String bizType;
    private String configKey;
    private String configValue;
    private String valueType;
    private String configName;
    private String remark;
}
